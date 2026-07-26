package nl.madebypatrick.flipiq.ui.scan

/**
 * One line of recognised text plus how tall it was in the frame.
 *
 * The height is the whole point: on a game/DVD/book cover the product name is the *biggest* text,
 * whereas the *longest* string is almost always a marketing blurb, a platform banner or publisher
 * legalese. Kept free of Android/ML Kit types so [bestTitleGuess] is a plain unit test.
 */
data class OcrLine(val text: String, val height: Int)

/**
 * Lines that appear on nearly every cover and are hardly ever the product name — publisher/platform
 * banners and legal text. Dropping these lets the height floor go lower (so a stylised title word
 * that OCRs a bit smaller, like the distressed "FIFA" over "STREET", still joins the title) without
 * dragging in "EA SPORTS" or "PlayStation Network".
 */
private val Boilerplate = listOf(
    "playstation", "nintendo", "xbox", "ea sports", "ubisoft", "activision", "bandai",
    "network", "blu-ray", "bluray", "dvd video", "pegi", "usk", "esrb",
    "only on", "exclusive", "official", "licensed", "all rights reserved", "www.", "made in", "©", "®", "™",
)

private val WhitespaceRun = Regex("\\s+")

/**
 * Lines within this fraction of the tallest line's height join the title. Deliberately generous
 * (0.55): cover titles are often two words at slightly different sizes, and after boilerplate is
 * stripped the remaining big text is almost always the title, so erring toward *fuller* text — which
 * the user can trim in the review field — beats collapsing to a single word.
 */
private const val SAME_SIZE_FLOOR = 0.55

/**
 * Best guess at the product name from a single OCR frame.
 *
 * Takes the tallest usable line and glues on any other line within [SAME_SIZE_FLOOR] of that height
 * (so multi-line / multi-size titles stay together), in reading order.
 *
 * Returns an **empty string** when nothing usable was recognised. Callers must treat that as "no
 * capture" rather than an empty title — see [FrontScanPane].
 */
fun bestTitleGuess(lines: List<OcrLine>, maxLength: Int = 80): String {
    val candidates = lines.filter { it.height > 0 && it.text.hasEnoughLetters() }
    if (candidates.isEmpty()) return ""

    // Prefer non-boilerplate lines, but never let the deny-list leave us empty-handed: plenty of
    // real titles legitimately contain "Nintendo", "Only On", and so on.
    val usable = candidates.filterNot { it.text.isBoilerplate() }.ifEmpty { candidates }

    val tallest = usable.maxByOrNull { it.height } ?: return ""
    val floor = tallest.height * SAME_SIZE_FLOOR

    return usable.filter { it.height >= floor }
        .joinToString(" ") { it.text }
        .replace(WhitespaceRun, " ")
        .trim()
        .take(maxLength)
        .trim()
}

/** Filters out age ratings, stray barcode digits and decorative glyphs. */
private fun String.hasEnoughLetters(): Boolean =
    trim().length >= 2 && count(Char::isLetter) >= 2

private fun String.isBoilerplate(): Boolean {
    val lower = lowercase()
    return Boilerplate.any { lower.contains(it) }
}
