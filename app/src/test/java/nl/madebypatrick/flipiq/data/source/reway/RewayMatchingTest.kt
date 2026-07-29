package nl.madebypatrick.flipiq.data.source.reway

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * The matching table from spec §4, driven off **real** saved `suggest.json` payloads (fetched once
 * on 2026-07-28, saved under `src/test/resources/reway/`) — no network. The whole feature lives or
 * dies here: identical titles differ 17–23× by platform, so a wrong pick is worse than no data.
 */
class RewayMatchingTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun load(name: String): List<RewayProductDto> {
        val text = javaClass.getResource("/reway/$name")!!.readText()
        return json.decodeFromString<RewaySuggestResponse>(text).products
    }

    private fun match(fixture: String, query: String, platform: String?, category: String? = null) =
        RewayMatching.bestMatch(query, platform, category, load(fixture))

    // --- The regression that defines the feature -----------------------------------------------

    @Test
    fun `God of War on PS2 is the €11,31 PS2 entry, not PS4 and not the Collection`() {
        val m = match("verkopen_god_of_war.json", "God of War", "Playstation 2")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("God of War")
        assertThat(m.vendor).isEqualTo("Playstation 2")
        assertThat(m.priceMax).isEqualTo("11.31")
    }

    @Test
    fun `God of War on PS4 is the €0,64 PS4 entry`() {
        val m = match("verkopen_god_of_war.json", "God of War", "Playstation 4")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("God of War")
        assertThat(m.vendor).isEqualTo("Playstation 4")
        assertThat(m.priceMax).isEqualTo("0.64")
    }

    @Test
    fun `God of War with an unknown platform resolves to null`() {
        assertThat(match("verkopen_god_of_war.json", "God of War", null)).isNull()
    }

    // --- The rest of the §4 table ---------------------------------------------------------------

    @Test
    fun `God of War Collection on PS2 is the Collection, not plain God of War`() {
        val m = match("verkopen_god_of_war.json", "God of War Collection", "Playstation 2")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("God of War Collection")
        assertThat(m.priceMax).isEqualTo("68.96")
    }

    @Test
    fun `Spiderman 3 on PS3 is €7,78, not the Wii entry`() {
        val m = match("verkopen_spiderman.json", "Spiderman 3", "Playstation 3")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("Spiderman 3")
        assertThat(m.vendor).isEqualTo("Playstation 3")
        assertThat(m.priceMax).isEqualTo("7.78")
    }

    @Test
    fun `Marvel Spiderman on PS4 is the plain entry, not Miles Morales`() {
        val m = match("verkopen_marvel_spiderman.json", "Marvel Spiderman", "Playstation 4")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("Marvel Spiderman")
        assertThat(m.priceMax).isEqualTo("1.88")
    }

    @Test
    fun `Marvel Spider-Man resolves despite the hyphen and is deterministic`() {
        val m = match("verkopen_marvel_spiderman.json", "Marvel Spider-Man", "Playstation 4")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("Marvel Spiderman")
        // Deterministic: same query, same winner every run.
        assertThat(match("verkopen_marvel_spiderman.json", "Marvel Spider-Man", "Playstation 4")?.priceMax)
            .isEqualTo(m.priceMax)
    }

    @Test
    fun `The Amazing Spiderman 2 on PS4 is the sequel`() {
        val m = match("retail_spiderman.json", "The Amazing Spiderman 2", "Playstation 4", "Games")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("The Amazing Spiderman 2")
        assertThat(m.priceMax).isEqualTo("49.95")
    }

    @Test
    fun `sequel number is decisive on the same platform - Amazing Spiderman 2 not plain`() {
        // Both plain "Amazing Spiderman" and "Amazing Spiderman 2" exist on 3DS; the numeric token wins.
        val m = match("verkopen_amazing_spiderman.json", "Amazing Spiderman 2", "3DS")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("Amazing Spiderman 2")
        assertThat(m.vendor).isEqualTo("3DS")
    }

    @Test
    fun `Spiderman on PS4 is never the LEGO set or the VTech cartridge`() {
        val m = match("retail_spiderman.json", "Spiderman", "Playstation 4", "Games")
        // May be a PS4 game or null, but never a Bouwsets / LEGO / VTech row.
        if (m != null) {
            assertThat(m.category).isNotEqualTo("Bouwsets")
            assertThat(m.vendor).isNotEqualTo("LEGO")
            assertThat(m.vendor).isNotEqualTo("VTech")
        }
    }

    @Test
    fun `FIFA 18 on PS4 matches despite casing siblings`() {
        val m = match("verkopen_fifa.json", "FIFA 18", "Playstation 4")
        assertThat(m).isNotNull()
        assertThat(m!!.title).isEqualTo("FIFA 18")
        assertThat(m.vendor).isEqualTo("Playstation 4")
        assertThat(m.priceMax).isEqualTo("0.31")
    }

    @Test
    fun `an unavailable candidate is never matched`() {
        val candidate = RewayProductDto(
            title = "God of War", vendor = "Playstation 2", type = "Games",
            priceMax = "11.31", available = false,
        )
        assertThat(RewayMatching.bestMatch("God of War", "Playstation 2", null, listOf(candidate))).isNull()
    }

    @Test
    fun `a zero price_max candidate is never matched`() {
        val candidate = RewayProductDto(
            title = "God of War", vendor = "Playstation 2", type = "Games",
            priceMax = "0.00", available = true,
        )
        assertThat(RewayMatching.bestMatch("God of War", "Playstation 2", null, listOf(candidate))).isNull()
    }

    @Test
    fun `ties resolve to the cheapest so the floor is never over-promised`() {
        val cheap = RewayProductDto(title = "Zelda", vendor = "Switch", type = "Games", priceMax = "10.00", available = true)
        val dear = RewayProductDto(title = "Zelda", vendor = "Switch", type = "Games", priceMax = "40.00", available = true)
        val m = RewayMatching.bestMatch("Zelda", "Switch", null, listOf(dear, cheap))
        assertThat(m?.priceMax).isEqualTo("10.00")
    }

    @Test
    fun `platformOf falls back to tags when vendor is missing`() {
        val dto = RewayProductDto(title = "X", vendor = null, tags = listOf("Games", "Playstation 4"))
        assertThat(RewayMatching.canonicalPlatform(RewayMatching.platformOf(dto))).isEqualTo("ps4")
    }
}
