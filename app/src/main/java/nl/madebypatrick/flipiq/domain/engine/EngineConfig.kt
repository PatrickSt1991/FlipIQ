package nl.madebypatrick.flipiq.domain.engine

import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition

/**
 * Tunable constants for the FlipIQ Engine. Extracted so the scoring model is transparent and can be
 * calibrated (or A/B'd) without touching the engine logic. Defaults are chosen so a strong flip —
 * healthy margin, sells fast, plenty of recent sales — lands in the 75–100 range.
 */
data class EngineConfig(
    // Deal Score weights (must sum to 1.0).
    val weightProfitability: Double = 0.45,
    val weightLiquidity: Double = 0.25,
    val weightVelocity: Double = 0.20,
    val weightTrend: Double = 0.10,

    /** When there are no sold comps, resale is estimated from asking prices × this factor. */
    val askingToSold: Double = 0.85,
    /** ROI at which the profitability component maxes out (1.0 = a 100% return scores full marks). */
    val roiForFullProfitability: Double = 1.0,
    /** Sold count at which the liquidity component maxes out. */
    val salesForFullLiquidity: Int = 15,
    /** Sold count at which confidence maxes out (before dispersion penalty). */
    val salesForFullConfidence: Int = 12,

    // Deal Score → tier thresholds.
    val tierBuyImmediately: Int = 90,
    val tierGreatDeal: Int = 75,
    val tierFairPrice: Int = 50,
    val tierLowProfit: Int = 25,

    // Buy-tier ladder, as fractions of net resale.
    val excellentFraction: Double = 0.50,
    val goodFraction: Double = 0.70,
    val fairFraction: Double = 0.85,

    // Sell speed thresholds, in average days between sales.
    val veryFastMaxDays: Double = 7.0,
    val fastMaxDays: Double = 14.0,
    val mediumMaxDays: Double = 56.0,
    /** Window assumed for sold data when listings carry no dates. */
    val defaultWindowDays: Int = 90,

    /** Relative change in recent-vs-older sold price that counts as a trend (0.05 = ±5%). */
    val trendThreshold: Double = 0.05,
) {
    /** Resale multiplier for condition, relative to a "GOOD" baseline (what typical sold data reflects). */
    fun conditionMultiplier(condition: Condition): Double = when (condition) {
        Condition.SEALED -> 1.20
        Condition.MINT -> 1.10
        Condition.GOOD -> 1.00
        Condition.ACCEPTABLE -> 0.85
        Condition.POOR -> 0.65
    }

    /** Resale multiplier for completeness, relative to a "COMPLETE" baseline. */
    fun completenessMultiplier(completeness: Completeness): Double = when (completeness) {
        Completeness.SEALED -> 1.15
        Completeness.COMPLETE -> 1.00
        Completeness.LOOSE -> 0.75
    }

    companion object {
        val DEFAULT = EngineConfig()
    }
}
