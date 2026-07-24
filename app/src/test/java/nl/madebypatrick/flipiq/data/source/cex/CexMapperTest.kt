package nl.madebypatrick.flipiq.data.source.cex

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import nl.madebypatrick.flipiq.domain.StaticCurrencyConverter
import nl.madebypatrick.flipiq.domain.model.Currency
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.Money
import org.junit.Test

class CexMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val samplePayload = """
        {
          "response": {
            "ack": "Success",
            "data": {
              "boxes": [
                {"boxId":"SLEG","boxName":"LEGO Jurassic World PS4","categoryName":"PS4 Games","sellPrice":7.0,"cashPrice":3.0,"exchangePrice":4.5},
                {"boxId":"SNOP","boxName":"No price box","sellPrice":0.0}
              ]
            }
          }
        }
    """.trimIndent()

    @Test
    fun `parses the nested cex response`() {
        val dto = json.decodeFromString<CexResponse>(samplePayload)
        assertThat(dto.response?.ack).isEqualTo("Success")
        assertThat(dto.response?.data?.boxes).hasSize(2)
    }

    @Test
    fun `maps buyable boxes to active listings and skips zero-priced ones`() {
        val result = json.decodeFromString<CexResponse>(samplePayload).toSourceResult()

        assertThat(result.available).isTrue()
        assertThat(result.listings).hasSize(1) // the 0.0 box is dropped
        val listing = result.listings.single()
        assertThat(listing.type).isEqualTo(ListingType.ACTIVE)
        assertThat(listing.price).isEqualTo(Money.ofEuros(7.0)) // still GBP magnitude here
        assertThat(result.productTitle).isEqualTo("LEGO Jurassic World PS4")
    }

    @Test
    fun `converts gbp listing prices to the eur base`() {
        val converter = StaticCurrencyConverter(mapOf(Currency.GBP to 1.15))
        val result = json.decodeFromString<CexResponse>(samplePayload)
            .toSourceResult()
            .toEurFromGbp(converter)

        assertThat(result.listings.single().price).isEqualTo(Money.ofCents(805)) // 700 * 1.15
    }

    @Test
    fun `a non-success ack is unavailable`() {
        val payload = """{"response":{"ack":"Fail","data":{"boxes":[]}}}"""
        val result = json.decodeFromString<CexResponse>(payload).toSourceResult()
        assertThat(result.available).isFalse()
        assertThat(result.listings).isEmpty()
    }

    @Test
    fun `an empty or missing response is unavailable`() {
        assertThat(CexResponse().toSourceResult().available).isFalse()
    }
}
