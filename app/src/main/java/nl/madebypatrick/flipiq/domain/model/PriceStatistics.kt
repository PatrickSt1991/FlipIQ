package nl.madebypatrick.flipiq.domain.model

/** Descriptive statistics over a set of sold prices, produced by the engine. */
data class PriceStatistics(
    val soldCount: Int,
    val average: Money,
    val median: Money,
    val lowest: Money,
    val highest: Money,
    /** Coefficient of variation (stddev / mean) of sold prices; 0 when fewer than two sales. */
    val dispersion: Double,
) {
    val hasData: Boolean get() = soldCount > 0

    companion object {
        val EMPTY = PriceStatistics(0, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, 0.0)
    }
}
