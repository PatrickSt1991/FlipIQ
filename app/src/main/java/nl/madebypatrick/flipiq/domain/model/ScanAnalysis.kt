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
 * Reway's Dutch buy-in/retail figures, surfaced on their own lines (§3/§8) and never blended into
 * the market estimate. All fields null when Reway had no (enabled) data for the item — the UI then
 * renders nothing for it (no "€0"). Computed outside the engine so scoring stays untouched.
 */
data class RewayInsight(
    /** Guaranteed buy-in — what Reway pays you (a payout, not a value). */
    val buyIn: Money? = null,
    val buyInUrl: String? = null,
    /** Reway's own retail asking price. */
    val retail: Money? = null,
    val retailUrl: String? = null,
    /**
     * A can't-lose buy price: the buy-in minus your minimum profit, **fees off** (there's no
     * marketplace cut on a trade-in). Kept visually distinct from the Profit-Mode recommended buy.
     */
    val guaranteedBuy: Money? = null,
) {
    val hasBuyIn get() = buyIn != null
    val hasRetail get() = retail != null
    val hasAny get() = hasBuyIn || hasRetail

    companion object {
        val EMPTY = RewayInsight()
    }
}

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
    val reway: RewayInsight = RewayInsight.EMPTY,
    /** True when every source is switched off in Settings — an explicit empty state (§7). */
    val allSourcesDisabled: Boolean = false,
)
