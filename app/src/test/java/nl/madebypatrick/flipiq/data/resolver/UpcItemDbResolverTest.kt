package nl.madebypatrick.flipiq.data.resolver

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class UpcItemDbResolverTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `parses and returns the first item title on OK`() {
        val payload = """
            {"code":"OK","total":1,"items":[
              {"title":"LEGO Jurassic World PS4","brand":"Warner"}
            ]}
        """.trimIndent()
        val dto = json.decodeFromString<UpcItemDbResponse>(payload)
        assertThat(dto.firstTitle()).isEqualTo("LEGO Jurassic World PS4")
    }

    @Test
    fun `returns null when the code is not OK`() {
        val payload = """{"code":"INVALID_UPC","items":[]}"""
        assertThat(json.decodeFromString<UpcItemDbResponse>(payload).firstTitle()).isNull()
    }

    @Test
    fun `returns null when there are no items`() {
        assertThat(UpcItemDbResponse(code = "OK", items = emptyList()).firstTitle()).isNull()
    }

    @Test
    fun `skips blank titles`() {
        val dto = UpcItemDbResponse(code = "OK", items = listOf(UpcItem(title = ""), UpcItem(title = "Real Title")))
        assertThat(dto.firstTitle()).isEqualTo("Real Title")
    }
}
