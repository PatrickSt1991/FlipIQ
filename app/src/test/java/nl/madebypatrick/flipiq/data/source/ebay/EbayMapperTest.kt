package nl.madebypatrick.flipiq.data.source.ebay

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.Money
import org.junit.Test

class EbayMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val payload = """
        {
          "itemSummaries": [
            {"title":"LEGO Jurassic World PS4","itemWebUrl":"https://ebay.nl/itm/1","price":{"value":"12.99","currency":"EUR"},"condition":"Used"},
            {"title":"LEGO Jurassic World PS4 (sealed)","itemWebUrl":"https://ebay.nl/itm/2","price":{"value":"24.50","currency":"EUR"}},
            {"title":"No price item","itemWebUrl":"https://ebay.nl/itm/3"}
          ]
        }
    """.trimIndent()

    @Test
    fun `maps priced item summaries to active listings and skips priceless ones`() {
        val result = json.decodeFromString<EbaySearchResponse>(payload).toSourceResult()

        assertThat(result.available).isTrue()
        assertThat(result.listings).hasSize(2)
        assertThat(result.listings.all { it.type == ListingType.ACTIVE }).isTrue()
        assertThat(result.listings.map { it.price })
            .containsExactly(Money.ofEuros(12.99), Money.ofEuros(24.50))
        assertThat(result.productTitle).isEqualTo("LEGO Jurassic World PS4")
        assertThat(result.shortcutUrl).isEqualTo("https://ebay.nl/itm/1")
    }

    @Test
    fun `an empty response is unavailable`() {
        assertThat(EbaySearchResponse().toSourceResult().available).isFalse()
    }
}
