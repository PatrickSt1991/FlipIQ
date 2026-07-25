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
                {"source":"marktplaats","title":"No price","price_cents":0,"type":"ASKING"}
              ],
              "providers": [{"provider":"marktplaats","available":true,"count":2}]
            }
        """.trimIndent()
        val dto = json.decodeFromString<EngineResponse>(payload)
        assertThat(dto.listings).hasSize(2)
        assertThat(dto.providers.first().provider).isEqualTo("marktplaats")
    }

    @Test
    fun `maps asking listings to active and drops zero prices`() {
        val resp = EngineResponse(
            listings = listOf(
                EngineListing(source = "marktplaats", title = "A", priceCents = 1500, type = "ASKING", url = "u"),
                EngineListing(source = "marktplaats", title = "Sold", priceCents = 900, type = "SOLD"),
                EngineListing(source = "marktplaats", title = "Zero", priceCents = 0, type = "ASKING"),
            ),
        )
        val result = resp.toSourceResult(shortcut = "https://marktplaats/search")

        assertThat(result.available).isTrue()
        assertThat(result.listings).hasSize(2) // zero dropped
        val byTitle = result.listings.associateBy { it.title }
        assertThat(byTitle["A"]?.type).isEqualTo(ListingType.ACTIVE)
        assertThat(byTitle["A"]?.price).isEqualTo(Money.ofCents(1500))
        assertThat(byTitle["Sold"]?.type).isEqualTo(ListingType.SOLD)
        assertThat(result.shortcutUrl).isEqualTo("https://marktplaats/search")
    }

    @Test
    fun `empty listings are unavailable`() {
        assertThat(EngineResponse().toSourceResult("s").available).isFalse()
    }
}
