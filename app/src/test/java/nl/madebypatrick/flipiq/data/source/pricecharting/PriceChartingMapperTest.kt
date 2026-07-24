package nl.madebypatrick.flipiq.data.source.pricecharting

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.Money
import org.junit.Test

class PriceChartingMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `parses the hyphenated PriceCharting fields`() {
        val payload = """
            {
              "status": "success",
              "id": "6910",
              "product-name": "Super Mario Bros",
              "console-name": "NES",
              "loose-price": 1234,
              "cib-price": 3456,
              "new-price": 9900,
              "upc": "045496630324",
              "some-unknown-field": "ignored"
            }
        """.trimIndent()

        val dto = json.decodeFromString<PriceChartingProductDto>(payload)

        assertThat(dto.status).isEqualTo("success")
        assertThat(dto.productName).isEqualTo("Super Mario Bros")
        assertThat(dto.cibPrice).isEqualTo(3456)
    }

    @Test
    fun `maps loose CIB and new prices to three sold listings with matching conditions`() {
        val dto = PriceChartingProductDto(
            status = "success",
            productName = "Super Mario Bros",
            consoleName = "NES",
            loosePrice = 1234,
            cibPrice = 3456,
            newPrice = 9900,
        )

        val result = dto.toSourceResult()

        assertThat(result.available).isTrue()
        assertThat(result.sourceId).isEqualTo(PriceChartingSource.SOURCE_ID)
        assertThat(result.listings).hasSize(3)
        assertThat(result.listings.all { it.type == ListingType.SOLD }).isTrue()

        val byCondition = result.listings.associateBy { it.condition }
        assertThat(byCondition[Condition.ACCEPTABLE]?.price).isEqualTo(Money.ofCents(1234))
        assertThat(byCondition[Condition.GOOD]?.price).isEqualTo(Money.ofCents(3456))
        assertThat(byCondition[Condition.SEALED]?.price).isEqualTo(Money.ofCents(9900))
    }

    @Test
    fun `omits missing or zero prices`() {
        val dto = PriceChartingProductDto(
            status = "success",
            productName = "Loose Only Cart",
            loosePrice = 500,
            cibPrice = 0,
            newPrice = null,
        )

        val result = dto.toSourceResult()

        assertThat(result.listings).hasSize(1)
        assertThat(result.listings.single().price).isEqualTo(Money.ofCents(500))
    }

    @Test
    fun `a non-success status is reported unavailable`() {
        val dto = PriceChartingProductDto(status = "error", productName = null)
        val result = dto.toSourceResult()
        assertThat(result.available).isFalse()
        assertThat(result.listings).isEmpty()
    }

    @Test
    fun `builds a title from product and console names`() {
        val dto = PriceChartingProductDto(
            status = "success",
            productName = "Halo 3",
            consoleName = "Xbox 360",
            cibPrice = 800,
        )
        assertThat(dto.toSourceResult().listings.first().title).isEqualTo("Halo 3 (Xbox 360)")
    }
}
