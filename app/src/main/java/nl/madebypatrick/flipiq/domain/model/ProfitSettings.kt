package nl.madebypatrick.flipiq.domain.model

/**
 * The user's personal flipping strategy. The engine adapts every recommendation to these values —
 * this is what "Profit Mode" in the README configures.
 */
data class ProfitSettings(
    /** Minimum absolute profit a flip must clear to be worth buying. */
    val minProfit: Money = Money.ofEuros(5.0),
    /** Minimum return on investment, as a fraction (0.30 = 30%). */
    val minRoi: Double = 0.30,
    /** Ignore items whose estimated resale is below this floor. */
    val ignoreBelow: Money = Money.ofEuros(10.0),
    /** Require at least this many recent sales before trusting the estimate. */
    val minSales: Int = 5,
    val ignoreIncomplete: Boolean = false,
    val ignoreDamaged: Boolean = true,
    val preferFastSellers: Boolean = true,
    val includeShipping: Boolean = true,
    val includeFees: Boolean = true,
    /** Marketplace fee as a fraction of the sale price (0.13 = 13%). */
    val marketplaceFee: Double = 0.13,
    /** Flat shipping cost the seller absorbs per sale. */
    val shippingCost: Money = Money.ofEuros(3.50),
) {
    companion object {
        val DEFAULT = ProfitSettings()
    }
}
