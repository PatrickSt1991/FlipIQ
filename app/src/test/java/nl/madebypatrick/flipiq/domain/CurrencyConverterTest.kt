package nl.madebypatrick.flipiq.domain

import com.google.common.truth.Truth.assertThat
import nl.madebypatrick.flipiq.domain.model.Currency
import nl.madebypatrick.flipiq.domain.model.Money
import org.junit.Test

class CurrencyConverterTest {

    private val converter = StaticCurrencyConverter(
        mapOf(Currency.USD to 0.90, Currency.GBP to 1.15),
    )

    @Test
    fun `euros pass through unchanged`() {
        assertThat(converter.toEur(Money.ofEuros(10.0), Currency.EUR)).isEqualTo(Money.ofEuros(10.0))
    }

    @Test
    fun `usd is converted at the configured rate`() {
        // 1000 USD-cents * 0.90 = 900 EUR-cents.
        assertThat(converter.toEur(Money.ofCents(1000), Currency.USD)).isEqualTo(Money.ofCents(900))
    }

    @Test
    fun `gbp is converted at the configured rate`() {
        assertThat(converter.toEur(Money.ofCents(1000), Currency.GBP)).isEqualTo(Money.ofCents(1150))
    }

    @Test
    fun `an unknown rate leaves the amount untouched rather than zeroing it`() {
        val bare = StaticCurrencyConverter(emptyMap())
        assertThat(bare.toEur(Money.ofCents(500), Currency.USD)).isEqualTo(Money.ofCents(500))
    }
}
