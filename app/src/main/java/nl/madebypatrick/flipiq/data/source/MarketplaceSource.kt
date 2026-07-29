package nl.madebypatrick.flipiq.data.source

import nl.madebypatrick.flipiq.domain.model.MarketListing

/** What we ask a marketplace about. */
data class ProductQuery(
    val barcode: String,
    val title: String? = null,
    /** Known platform (e.g. from the barcode resolver or cover scan). Reway needs it to match (§4). */
    val platform: String? = null,
    /** Known category (e.g. "Games"); lets platform-agnostic sources reject cross-category traps. */
    val category: String? = null,
    /** True during a bulk Haul scan, so latency-sensitive/retail sources can opt out (§6). */
    val haul: Boolean = false,
)

/** What a single marketplace returns for a query. */
data class SourceResult(
    val sourceId: String,
    val listings: List<MarketListing>,
    val productTitle: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    /** Whether the source had any data for this item at all. */
    val available: Boolean = true,
    /** Deep link to view this item on the marketplace (the "open with one tap" shortcut). */
    val shortcutUrl: String? = null,
)

/**
 * A source of price data for one marketplace (eBay, CeX, PriceCharting, Vinted, Marktplaats, …).
 *
 * This is the seam the whole app is built around: today every implementation is backed by mock
 * fixture data, but each can be swapped for a real API/scraper independently — the repository,
 * engine and UI never change. Implementations must be safe to call concurrently and should return
 * an empty, `available = false` result rather than throwing when they have nothing.
 */
interface MarketplaceSource {
    val id: String
    val displayName: String

    suspend fun lookup(query: ProductQuery): SourceResult
}
