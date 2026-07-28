package nl.madebypatrick.flipiq.ui.share

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * This file is the spec for [sharedTitle] / [sharedBarcode]. If an app changes its share wording,
 * add the new string here first, then make it pass. Style matches `TitleGuessTest`.
 */
class SharedTextTest {

    @Test
    fun `quoted name wins immediately (Marktplaats)`() {
        val shared = "Bekijk 'LEGO Jurassic World PS4' op Marktplaats " +
            "https://link.marktplaats.nl/m2145678901"
        assertThat(sharedTitle(shared)).isEqualTo("LEGO Jurassic World PS4")
    }

    @Test
    fun `leftover prose after eBay filler (bare numeric url)`() {
        val shared = "Check out this item I found on eBay: The Last of Us Part II PS4 " +
            "https://www.ebay.co.uk/itm/123456789012"
        assertThat(sharedTitle(shared)).isEqualTo("The Last of Us Part II PS4")
    }

    @Test
    fun `vinted url slug when prose is all filler`() {
        val shared = "Bekijk dit item op Vinted: " +
            "https://www.vinted.nl/items/4567890123-fifa-street-ps2"
        assertThat(sharedTitle(shared)).isEqualTo("fifa street ps2")
    }

    @Test
    fun `marktplaats pipe-separated title keeps only the first segment (issue 59)`() {
        val shared = "Super Mario Party | Nintendo Switch | Als Nieuw.\n" +
            "€ 35,00\n" +
            "https://link.marktplaats.nl/m2407077198?utm_source=android_social&utm_medium=android_social"
        assertThat(sharedTitle(shared)).isEqualTo("Super Mario Party")
    }

    @Test
    fun `old-style eBay itm slug in an earlier path segment`() {
        val shared = "https://www.ebay.com/itm/LEGO-Technic-Bugatti-Chiron-42083/" +
            "183456789012?hash=item2a"
        assertThat(sharedTitle(shared)).isEqualTo("LEGO Technic Bugatti Chiron")
    }

    @Test
    fun `prose wins over slug when they share a word (keeps casing and punctuation)`() {
        val shared = "LEGO Jurassic World (PS4) | Marktplaats\n" +
            "https://link.marktplaats.nl/m2145678901-lego-jurassic-world-ps4"
        assertThat(sharedTitle(shared)).isEqualTo("LEGO Jurassic World (PS4)")
    }

    @Test
    fun `slug wins when prose is chatter we have no pattern for`() {
        val shared = "moet je dit ff zien!! " +
            "https://link.marktplaats.nl/m2145678901-lego-star-wars-the-skywalker-saga-ps5"
        assertThat(sharedTitle(shared)).isEqualTo("lego star wars the skywalker saga ps5")
    }

    @Test
    fun `strips a trailing price from a quoted name`() {
        val shared = "Bekijk 'Nintendo Switch OLED' op Marktplaats - € 249,00 " +
            "https://link.marktplaats.nl/m999"
        assertThat(sharedTitle(shared)).isEqualTo("Nintendo Switch OLED")
    }

    @Test
    fun `pure app chatter with a bare-id url resolves to nothing`() {
        assertThat(sharedTitle("Shared from the eBay app https://ebay.us/xYz123")).isNull()
    }

    @Test
    fun `a plain title passes through unchanged`() {
        assertThat(sharedTitle("The Last of Us Part II")).isEqualTo("The Last of Us Part II")
    }

    @Test
    fun `a marktplaats category (v) url is not a title`() {
        assertThat(sharedTitle("https://www.marktplaats.nl/v/games-playstation-4/m2145678901"))
            .isNull()
    }

    @Test
    fun `a marktplaats browse (l) url is not a title`() {
        assertThat(sharedTitle("https://www.marktplaats.nl/l/games-playstation-4/")).isNull()
    }

    @Test
    fun `caps at maxLength`() {
        val shared = "A".repeat(30) + " " + "B".repeat(60)
        assertThat(sharedTitle(shared, maxLength = 40)).hasLength(40)
    }

    @Test
    fun `bare EAN is recovered as a barcode`() {
        assertThat(sharedBarcode("5051888223451")).isEqualTo("5051888223451")
    }

    @Test
    fun `barcode is trimmed of surrounding whitespace`() {
        assertThat(sharedBarcode(" 0711719417972 \n")).isEqualTo("0711719417972")
    }

    @Test
    fun `digits with any other text are not a barcode`() {
        assertThat(sharedBarcode("EAN 5051888223451 for sale")).isNull()
        assertThat(sharedBarcode("https://ebay.us/1234567890")).isNull()
    }

    @Test
    fun `too few digits is not a barcode`() {
        assertThat(sharedBarcode("1234")).isNull()
    }
}
