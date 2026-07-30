package nl.madebypatrick.flipiq.domain.model

/** Identifying details about a scanned product, resolved from the data sources. */
data class ProductInfo(
    val barcode: String,
    val title: String,
    val category: String? = null,
    val imageUrl: String? = null,
)

/** Per-source outcome, surfaced so the UI can show which marketplaces had data. */
data class SourceOutcome(
    val sourceId: String,
    val displayName: String,
    val listingCount: Int,
    val available: Boolean,
    val shortcutUrl: String?,
)

/**
 * What one marketplace actually contributed to the price picture, so the result screen can show
 * *where each price came from* instead of only a blended total. [sold] distinguishes completed-sale
 * comps (eBay sold) from active asking prices (Marktplaats, eBay active, Discogs).
 */
data class SourcePriceGroup(
    val sourceId: String,
    val displayName: String,
    val count: Int,
    val low: Money,
    val median: Money,
    val high: Money,
    val sold: Boolean,
)

/**
 * The complete result of scanning and analysing one item: what it is, what each marketplace
 * returned, and the engine's verdict. This is what the ViewModel hands to the result screen.
 */
data class ScanAnalysis(
    val product: ProductInfo,
    val recommendation: FlipRecommendation,
    val sources: List<SourceOutcome>,
    val condition: Condition,
    val completeness: Completeness,
    /** Per-marketplace price breakdown for the result screen ("where did each price come from"). */
    val pricesBySource: List<SourcePriceGroup> = emptyList(),
    /** True when every source is switched off in Settings — an explicit empty state (§7). */
    val allSourcesDisabled: Boolean = false,
)
