package nl.madebypatrick.flipiq.domain.model

/** One rung of the "buy below €X" ladder shown on the result screen. */
data class BuyTier(
    val level: BuyTierLevel,
    val maxPrice: Money,
)

/** The 0–100 Deal Score plus its bucket. */
data class DealScore(
    val value: Int,
    val tier: DealTier,
)

/**
 * Everything the FlipIQ Engine produces for a scanned item. This is the single object the result
 * screen renders and the answer to the only question that matters: "Should I buy this?"
 */
data class FlipRecommendation(
    val stats: PriceStatistics,
    /** Condition/completeness-adjusted estimate of what the item resells for (gross). */
    val estimatedResale: Money,
    /** What the seller actually nets after fees and shipping are removed. */
    val netResale: Money,
    /** Highest price you can pay and still hit the profit + ROI targets. */
    val recommendedBuyPrice: Money,
    /** Expected profit when buying at [recommendedBuyPrice]. */
    val expectedProfit: Money,
    /** ROI at [recommendedBuyPrice], as a fraction (0.46 = 46%). */
    val roi: Double,
    val dealScore: DealScore,
    val sellSpeed: SellSpeed,
    /** Market confidence 0–100. */
    val confidence: Int,
    val trend: MarketTrend,
    val buyTiers: List<BuyTier>,
    /** False when the user's Profit Mode rules rule the item out (too cheap, damaged, too few sales…). */
    val viable: Boolean,
    /** Human-readable reasons behind [viable]/scoring, for display and debugging. */
    val notes: List<String>,
) {
    val roiPercent: Int get() = (roi * 100).toInt()
}

/** The scanned item plus context the engine needs to evaluate it. */
data class EngineInput(
    val listings: List<MarketListing>,
    val condition: Condition = Condition.GOOD,
    val completeness: Completeness = Completeness.COMPLETE,
    val settings: ProfitSettings = ProfitSettings.DEFAULT,
    /** Optional: the price this specific item is being offered to you at, to score the actual deal. */
    val askingPrice: Money? = null,
)
