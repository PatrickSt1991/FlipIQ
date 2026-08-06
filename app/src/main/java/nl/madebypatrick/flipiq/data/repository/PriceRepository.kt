package nl.madebypatrick.flipiq.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.data.diagnostics.DiagnosticsLog
import nl.madebypatrick.flipiq.data.resolver.BarcodeResolver
import nl.madebypatrick.flipiq.data.resolver.EandataResolver
import nl.madebypatrick.flipiq.data.settings.SettingsRepository
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
import nl.madebypatrick.flipiq.domain.model.SourcePriceGroup

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
    private val settingsRepository: SettingsRepository,
    private val barcodeResolver: BarcodeResolver,
    private val eandata: EandataResolver,
    private val appScope: CoroutineScope,
) {

    /** Search by a free-text title (e.g. from OCR of the product's front) instead of a barcode. */
    suspend fun fetchByTitle(title: String): FetchedMarket =
        fetchInternal(barcode = "", query = ProductQuery(barcode = "", title = title))

    suspend fun fetch(barcode: String): FetchedMarket {
        // Fast path: hand the barcode to the engine, which resolves it server-side (cached) via its
        // keyless sources (ean13/buycott/…) and returns prices in one round-trip.
        val fromEngine = fetchInternal(barcode, ProductQuery(barcode, title = null))
        if (fromEngine.allSourcesDisabled) return fromEngine

        if (fromEngine.product.title != UNKNOWN_TITLE) {
            // Engine resolved it → give the title back to eandata if they lack it (fire-and-forget,
            // earns lookup credits and helps their DB; their manual review guards against bad data).
            if (barcode.isNotBlank()) {
                appScope.launch { eandata.contributeIfMissing(barcode, fromEngine.product.title) }
            }
            return fromEngine
        }

        // Engine couldn't resolve the barcode. The engine runs from Cloudflare's shared egress IP,
        // whose UPCitemdb trial quota is often exhausted (and eandata blocks it outright) — so retry
        // the same keyless sources ON-DEVICE, where the phone's residential IP has its own fresh
        // quota. This is what turns "unknown" back into a title (and prices) for e.g. PS4/PS5 games.
        DiagnosticsLog.log("engine miss for $barcode → resolving on-device (UPCitemdb/EAN-Search)")
        val onDeviceTitle = runCatching { barcodeResolver.resolveTitle(barcode) }.getOrNull()
        if (!onDeviceTitle.isNullOrBlank()) {
            DiagnosticsLog.log("on-device resolved $barcode → '$onDeviceTitle'")
            appScope.launch { eandata.contributeIfMissing(barcode, onDeviceTitle) }
            return fetchInternal(barcode, ProductQuery(barcode, title = onDeviceTitle))
        }

        // Still nothing → last-resort eandata (residential IP; can fill gaps UPCitemdb misses).
        DiagnosticsLog.log("on-device miss for $barcode → trying eandata")
        val title = runCatching { eandata.resolveTitle(barcode) }.getOrNull()
        return if (!title.isNullOrBlank()) {
            DiagnosticsLog.log("eandata resolved $barcode → '$title'")
            fetchInternal(barcode, ProductQuery(barcode, title = title))
        } else {
            DiagnosticsLog.log("eandata also missed $barcode → unknown item")
            fromEngine
        }
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
                product = ProductInfo(barcode = barcode, title = query.title ?: UNKNOWN_TITLE),
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
            title = resolvedTitle ?: UNKNOWN_TITLE,
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
            pricesBySource = pricesBySource(market.listings),
            allSourcesDisabled = market.allSourcesDisabled,
        )
    }

    /**
     * Group the raw listings by the marketplace they came from and summarise each — count + low /
     * median / high — so the UI can show where every price originated. Ordered most-data-first.
     */
    private fun pricesBySource(listings: List<MarketListing>): List<SourcePriceGroup> =
        listings.groupBy { it.sourceId }
            .map { (sourceId, group) ->
                val cents = group.map { it.price.cents }.sorted()
                SourcePriceGroup(
                    sourceId = sourceId,
                    displayName = sourceDisplayName(sourceId),
                    count = group.size,
                    low = Money(cents.first()),
                    median = Money(cents[cents.size / 2]),
                    high = Money(cents.last()),
                    // A group is "sold" comps when most of its points are completed sales.
                    sold = group.count { it.isSold } * 2 >= group.size,
                )
            }
            .sortedByDescending { it.count }

    /** Marketplace brand name for a source id (brand names aren't translated; the UI adds a badge). */
    private fun sourceDisplayName(id: String): String = when (id) {
        "marktplaats" -> "Marktplaats"
        "ebay", "ebay_sold" -> "eBay"
        "discogs" -> "Discogs"
        else -> id.replaceFirstChar { it.uppercase() }
    }

    /** Convenience: fetch then evaluate in one call. */
    suspend fun analyze(
        barcode: String,
        condition: Condition = Condition.GOOD,
        completeness: Completeness = Completeness.COMPLETE,
        settings: ProfitSettings = ProfitSettings.DEFAULT,
        askingPrice: Money? = null,
    ): ScanAnalysis = evaluate(fetch(barcode), condition, completeness, settings, askingPrice)

    private companion object {
        /** Placeholder title used when nothing resolved the barcode — the eandata fallback trigger. */
        const val UNKNOWN_TITLE = "Unknown item"
    }
}
