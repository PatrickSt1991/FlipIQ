package nl.madebypatrick.flipiq.data.source.reway

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A single parser must accept both endpoint shapes (§2/§4): decimal strings from `suggest.json` /
 * `products.json` (`"23.95"`, `"0,31"`) and integer cents from `/products/x.js` (`1200`).
 */
class RewayPriceParsingTest {

    @Test
    fun `decimal string with a dot parses to cents`() {
        assertThat(RewayMatching.parsePriceCents("23.95")).isEqualTo(2395L)
    }

    @Test
    fun `decimal string with a comma parses to cents`() {
        assertThat(RewayMatching.parsePriceCents("0,31")).isEqualTo(31L)
    }

    @Test
    fun `integer cents from the products js endpoint pass through`() {
        assertThat(RewayMatching.parsePriceCents(1200L)).isEqualTo(1200L)
        // The same value arriving as an unpunctuated string is already cents, not euros.
        assertThat(RewayMatching.parsePriceCents("1200")).isEqualTo(1200L)
    }

    @Test
    fun `zero and negative prices are rejected`() {
        assertThat(RewayMatching.parsePriceCents("0.00")).isNull()
        assertThat(RewayMatching.parsePriceCents("0")).isNull()
        assertThat(RewayMatching.parsePriceCents(0L)).isNull()
        assertThat(RewayMatching.parsePriceCents(-5L)).isNull()
    }

    @Test
    fun `null and blank are null`() {
        assertThat(RewayMatching.parsePriceCents(null as String?)).isNull()
        assertThat(RewayMatching.parsePriceCents("  ")).isNull()
        assertThat(RewayMatching.parsePriceCents(null as Long?)).isNull()
    }

    @Test
    fun `normalise strips hyphens accents and noise, keeps sequel numbers`() {
        assertThat(RewayMatching.normalise("Marvel Spider-Man")).containsExactly("marvel", "spiderman")
        assertThat(RewayMatching.normalise("The Amazing Spiderman 2")).containsExactly("amazing", "spiderman", "2")
        // Leading LEGO catalogue number dropped; sequel numbers kept.
        assertThat(RewayMatching.normalise("71343 Fun Pack")).containsExactly("fun", "pack")
    }
}
