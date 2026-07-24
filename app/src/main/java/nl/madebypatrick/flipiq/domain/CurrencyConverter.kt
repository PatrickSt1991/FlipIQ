package nl.madebypatrick.flipiq.domain

import nl.madebypatrick.flipiq.domain.model.Currency
import nl.madebypatrick.flipiq.domain.model.Money

/** Converts prices from any [Currency] into the EUR base the rest of the app works in. */
interface CurrencyConverter {
    fun toEur(amount: Money, from: Currency): Money
}

/**
 * Fixed-rate converter. Rates are "EUR per 1 unit of the foreign currency" (e.g. USD → 0.92).
 * A live FX-rate source is a follow-up; the static rates keep foreign prices in the right ballpark
 * so a USD source doesn't wildly distort recommendations against the EUR marketplaces.
 */
class StaticCurrencyConverter(
    private val eurPerUnit: Map<Currency, Double> = DEFAULT_RATES,
) : CurrencyConverter {

    override fun toEur(amount: Money, from: Currency): Money {
        if (from == Currency.EUR) return amount
        val rate = eurPerUnit[from] ?: return amount
        return amount * rate
    }

    companion object {
        val DEFAULT_RATES = mapOf(
            Currency.USD to 0.92,
            Currency.GBP to 1.17,
        )
    }
}
