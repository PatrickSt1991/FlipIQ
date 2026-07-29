package nl.madebypatrick.flipiq.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import nl.madebypatrick.flipiq.data.resolver.BarcodeResolver
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.ShortcutOnlySource
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.data.source.reway.RewaySource
import nl.madebypatrick.flipiq.domain.engine.FlipIQEngine
import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.EngineInput
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ProductInfo
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.domain.model.RewayInsight
import nl.madebypatrick.flipiq.domain.model.ScanAnalysis
import nl.madebypatrick.flipiq.domain.model.SourceOutcome

/** Raw market data fetched for a barcode, before any engine scoring. */
data class FetchedMarket(
    val product: ProductInfo,
    val listings: List<MarketListing>,
    val sources: List<SourceOutcome>,
    /** True when the user has switched every source off in Settings — surfaced as an empty state. */
    val allSourcesDisabled: Boolean = false,
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
    private val settingsRepository: SettingsRepository,
) {

    /** Search by a free-text title (e.g. from OCR of the product's front) instead of a barcode. */
    suspend fun fetchByTitle(title: String): FetchedMarket =
        fetchInternal(barcode = "", query = ProductQuery(barcode = "", title = title))

    suspend fun fetch(barcode: String): FetchedMarket {
        // Resolve a product name first so name-based sources (CeX, shortcuts) can match on it.
        val lookedUpTitle = runCatching { barcodeResolver.resolveTitle(barcode) }.getOrNull()
        return fetchInternal(barcode, ProductQuery(barcode, title = lookedUpTitle))
    }

    private suspend fun fetchInternal(barcode: String, query: ProductQuery): FetchedMarket = coroutineScope {
        // Resolve the active set here, not in DI: the graph is built once, so a Settings toggle would
        // otherwise need a process restart to take effect (§7). Deny-list semantics — filter *out*.
        val disabled = runCatching { settingsRepository.disabledSourceIds.first() }.getOrDefault(emptySet())
        val active = sources.filterNot { it.id in disabled }

        // Everything switched off → an explicit empty state that points at Settings, not a fetch that
        // renders a generic "no data" screen (§7). Don't block the last toggle; just explain it.
        if (active.isEmpty()) {
            return@coroutineScope FetchedMarket(
                product = ProductInfo(barcode = barcode, title = query.title ?: "Unknown item"),
                listings = emptyList(),
                sources = emptyList(),
                allSourcesDisabled = true,
            )
        }

        val results: List<SourceResult> = active
            .map { source -> async { runCatching { source.lookup(query) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()

        // Prefer a title a data source returned; otherwise the query title (barcode-lookup or OCR).
        val resolvedTitle = results.firstNotNullOfOrNull { it.productTitle } ?: query.title
        val product = ProductInfo(
            barcode = barcode,
            title = resolvedTitle ?: "Unknown item",
            category = results.firstNotNullOfOrNull { it.category },
            imageUrl = results.firstNotNullOfOrNull { it.imageUrl },
        )

        // Search term for shortcut links: the resolved product name if we have one, else the barcode.
        val shortcutQuery = resolvedTitle ?: barcode

        // Preserve configured source order for stable UI, regardless of which finished first. Build
        // from `active`, NOT `sources` — a disabled source must not render as an `available = false`
        // row (which reads as "broken" rather than "you turned this off"). (§7)
        val outcomeById = results.associateBy { it.sourceId }
        val outcomes = active.map { source ->
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
            reway = rewayInsight(market, settings),
            allSourcesDisabled = market.allSourcesDisabled,
        )
    }

    /**
     * Reway's buy-in/retail figures for the result screen (§3/§8). Read straight off the fetched
     * listings by source id — the buy-in is a [ListingType.TRADE_IN] point the engine never scored.
     * When a Reway source is switched off it isn't fetched, so its listing is simply absent and the
     * corresponding field stays null (the UI renders nothing — no "€0").
     */
    private fun rewayInsight(market: FetchedMarket, settings: ProfitSettings): RewayInsight {
        val buyIn = market.listings.firstOrNull {
            it.sourceId == RewaySource.BUY_IN_ID && it.type == ListingType.TRADE_IN
        }?.price
        val retail = market.listings.firstOrNull { it.sourceId == RewaySource.RETAIL_ID }?.price
        if (buyIn == null && retail == null) return RewayInsight.EMPTY
        fun urlOf(id: String) = market.sources.firstOrNull { it.sourceId == id }?.shortcutUrl
        return RewayInsight(
            buyIn = buyIn,
            buyInUrl = urlOf(RewaySource.BUY_IN_ID),
            retail = retail,
            retailUrl = urlOf(RewaySource.RETAIL_ID),
            // Can't-lose buy = buy-in minus your min profit, fees off (no marketplace cut).
            guaranteedBuy = buyIn?.let { (it - settings.minProfit).coerceAtLeastZero() },
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
