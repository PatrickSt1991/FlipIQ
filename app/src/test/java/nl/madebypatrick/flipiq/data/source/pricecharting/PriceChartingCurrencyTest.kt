package nl.madebypatrick.flipiq.data.source.pricecharting

import com.google.common.truth.Truth.assertThat
import nl.madebypatrick.flipiq.domain.StaticCurrencyConverter
import nl.madebypatrick.flipiq.domain.model.Currency
import nl.madebypatrick.flipiq.domain.model.Money
import org.junit.Test

class PriceChartingCurrencyTest {

    @Test
    fun `toEur converts every listing price from usd to the eur base`() {
        val usdResult = PriceChartingProductDto(
            status = "success",
            productName = "Halo 3",
            cibPrice = 1000, // $10.00
            newPrice = 2000, // $20.00
        ).toSourceResult()

        val converter = StaticCurrencyConverter(mapOf(Currency.USD to 0.90))
        val eur = usdResult.toEur(converter)

        assertThat(eur.listings.map { it.price }).containsExactly(Money.ofCents(900), Money.ofCents(1800))
    }
}
