package nl.madebypatrick.flipiq.ui.scan

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TitleGuessTest {

    @Test
    fun `picks the biggest text, not the longest line`() {
        val lines = listOf(
            OcrLine("LEGO Jurassic World", 64),
            OcrLine("Includes bonus levels and characters from all four films", 12),
            OcrLine("PlayStation 4", 20),
        )
        assertThat(bestTitleGuess(lines)).isEqualTo("LEGO Jurassic World")
    }

    @Test
    fun `keeps a multi-line title together`() {
        val lines = listOf(
            OcrLine("The Last of Us", 60),
            OcrLine("Part II", 54),
            OcrLine("Naughty Dog", 14),
        )
        assertThat(bestTitleGuess(lines)).isEqualTo("The Last of Us Part II")
    }

    @Test
    fun `joins a stylised two-size title and drops publisher banners (FIFA Street)`() {
        // Real issue #33 cover: distressed "FIFA" OCRs smaller than "STREET"; banners surround it.
        // Old 0.75 floor kept only "STREET"; the 0.55 floor + boilerplate list recovers "FIFA STREET".
        val lines = listOf(
            OcrLine("EA SPORTS", 30),
            OcrLine("FIFA", 40),
            OcrLine("STREET", 60),
            OcrLine("PlayStation Network", 18),
            OcrLine("3", 44), // PEGI age badge — no letters, filtered
        )
        assertThat(bestTitleGuess(lines)).isEqualTo("FIFA STREET")
    }

    @Test
    fun `returns blank when nothing usable was read`() {
        assertThat(bestTitleGuess(emptyList())).isEmpty()
        assertThat(bestTitleGuess(listOf(OcrLine("18", 40), OcrLine("©", 10)))).isEmpty()
    }

    @Test
    fun `falls back to boilerplate rather than returning nothing`() {
        val lines = listOf(OcrLine("Nintendo Switch Sports", 50))
        assertThat(bestTitleGuess(lines)).isEqualTo("Nintendo Switch Sports")
    }

    @Test
    fun `collapses whitespace and caps the length`() {
        assertThat(bestTitleGuess(listOf(OcrLine("Gran   Turismo    7", 40))))
            .isEqualTo("Gran Turismo 7")
        assertThat(bestTitleGuess(listOf(OcrLine("a".repeat(200), 40)))).hasLength(80)
    }
}
