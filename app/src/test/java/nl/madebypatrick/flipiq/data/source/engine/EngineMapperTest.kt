package nl.madebypatrick.flipiq.data.source.engine

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.Money
import org.junit.Test

class EngineMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `parses the engine response`() {
        val payload = """
            {
              "query": "lego jurassic world",
              "listings": [
                {"source":"marktplaats","title":"LEGO Jurassic World","price_cents":1500,"currency":"EUR","type":"ASKING","url":"https://www.marktplaats.nl/v/x"},
                {"source":"ebay","title":"LEGO JW eBay","price_cents":1299,"currency":"EUR","type":"ASKING"}
              ],
              "providers": [{"provider":"marktplaats","available":true,"count":1},{"provider":"ebay","available":true,"count":1}]
            }
        """.trimIndent()
        val dto = json.decodeFromString<EngineResponse>(payload)
        assertThat(dto.listings).hasSize(2)
        assertThat(dto.providers.map { it.provider }).containsExactly("marktplaats", "ebay")
    }

    @Test
    fun `tags each listing with its marketplace and drops zero prices`() {
        val resp = EngineResponse(
            listings = listOf(
                EngineListing(source = "marktplaats", title = "A", priceCents = 1500, type = "ASKING", url = "u"),
                EngineListing(source = "ebay", title = "B", priceCents = 1299, type = "ASKING"),
                EngineListing(source = "marktplaats", title = "Zero", priceCents = 0, type = "ASKING"),
            ),
        )
        val result = resp.toSourceResult()

        assertThat(result.available).isTrue()
        assertThat(result.sourceId).isEqualTo("engine")
        assertThat(result.shortcutUrl).isNull()
        assertThat(result.listings).hasSize(2) // zero dropped
        val byTitle = result.listings.associateBy { it.title }
        assertThat(byTitle["A"]?.sourceId).isEqualTo("marktplaats")
        assertThat(byTitle["A"]?.type).isEqualTo(ListingType.ACTIVE)
        assertThat(byTitle["A"]?.price).isEqualTo(Money.ofCents(1500))
        assertThat(byTitle["B"]?.sourceId).isEqualTo("ebay")
    }

    @Test
    fun `empty listings are unavailable`() {
        assertThat(EngineResponse().toSourceResult().available).isFalse()
    }
}
