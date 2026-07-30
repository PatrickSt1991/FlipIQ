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
 * The complete result of scanning and analysing one item: what it is, what each marketplace
 * returned, and the engine's verdict. This is what the ViewModel hands to the result screen.
 */
data class ScanAnalysis(
    val product: ProductInfo,
    val recommendation: FlipRecommendation,
    val sources: List<SourceOutcome>,
    val condition: Condition,
    val completeness: Completeness,
    /** True when every source is switched off in Settings — an explicit empty state (§7). */
    val allSourcesDisabled: Boolean = false,
)
