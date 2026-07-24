package nl.madebypatrick.flipiq.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import nl.madebypatrick.flipiq.data.resolver.BarcodeResolver
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.ShortcutOnlySource
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.domain.engine.FlipIQEngine
import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.EngineInput
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ProductInfo
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.domain.model.ScanAnalysis
import nl.madebypatrick.flipiq.domain.model.SourceOutcome

/** Raw market data fetched for a barcode, before any engine scoring. */
data class FetchedMarket(
    val product: ProductInfo,
    val listings: List<MarketListing>,
    val sources: List<SourceOutcome>,
)

/**
 * Aggregates every [MarketplaceSource], merges their listings, and runs the [FlipIQEngine] to turn
 * a barcode into a buy/skip verdict.
 *
 * [fetch] does the (concurrent, failure-isolated) network work once; [evaluate] is pure and cheap,
 * so the UI can re-score instantly as the user tweaks condition, completeness or their asking price
 * without hitting the sources again.
 */
class PriceRepository(
    private val sources: List<MarketplaceSource>,
    private val engine: FlipIQEngine,
    private val barcodeResolver: BarcodeResolver,
) {

    suspend fun fetch(barcode: String): FetchedMarket = coroutineScope {
        // Resolve a product name first so name-based sources (CeX, shortcuts) can match on it.
        val lookedUpTitle = runCatching { barcodeResolver.resolveTitle(barcode) }.getOrNull()
        val query = ProductQuery(barcode, title = lookedUpTitle)

        val results: List<SourceResult> = sources
            .map { source -> async { runCatching { source.lookup(query) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()

        // Prefer a title a data source returned; otherwise the barcode-lookup result.
        val resolvedTitle = results.firstNotNullOfOrNull { it.productTitle } ?: lookedUpTitle
        val product = ProductInfo(
            barcode = barcode,
            title = resolvedTitle ?: "Unknown item",
            category = results.firstNotNullOfOrNull { it.category },
            imageUrl = results.firstNotNullOfOrNull { it.imageUrl },
        )

        // Search term for shortcut links: the resolved product name if we have one, else the barcode.
        val shortcutQuery = resolvedTitle ?: barcode

        // Preserve configured source order for stable UI, regardless of which finished first.
        val outcomeById = results.associateBy { it.sourceId }
        val outcomes = sources.map { source ->
            val r = outcomeById[source.id]
            SourceOutcome(
                sourceId = source.id,
                displayName = source.displayName,
                listingCount = r?.listings?.size ?: 0,
                available = r?.available ?: false,
                // Shortcut-only marketplaces get a title-based search link built here.
                shortcutUrl = if (source is ShortcutOnlySource) source.shortcutFor(shortcutQuery) else r?.shortcutUrl,
            )
        }

        FetchedMarket(product, results.flatMap { it.listings }, outcomes)
    }

    fun evaluate(
        market: FetchedMarket,
        condition: Condition = Condition.GOOD,
        completeness: Completeness = Completeness.COMPLETE,
        settings: ProfitSettings = ProfitSettings.DEFAULT,
        askingPrice: Money? = null,
    ): ScanAnalysis {
        val recommendation = engine.evaluate(
            EngineInput(
                listings = market.listings,
                condition = condition,
                completeness = completeness,
                settings = settings,
                askingPrice = askingPrice,
            ),
        )
        return ScanAnalysis(
            product = market.product,
            recommendation = recommendation,
            sources = market.sources,
            condition = condition,
            completeness = completeness,
        )
    }

    /** Convenience: fetch then evaluate in one call. */
    suspend fun analyze(
        barcode: String,
        condition: Condition = Condition.GOOD,
        completeness: Completeness = Completeness.COMPLETE,
        settings: ProfitSettings = ProfitSettings.DEFAULT,
        askingPrice: Money? = null,
    ): ScanAnalysis = evaluate(fetch(barcode), condition, completeness, settings, askingPrice)
}
