package nl.madebypatrick.flipiq.domain.model

/**
 * Currencies FlipIQ understands. **EUR is the base**: every price is normalised to euros before it
 * reaches the engine, so all downstream math and display is single-currency.
 */
enum class Currency(val code: String, val symbol: String) {
    EUR("EUR", "€"),
    USD("USD", "$"),
    GBP("GBP", "£"),
}
