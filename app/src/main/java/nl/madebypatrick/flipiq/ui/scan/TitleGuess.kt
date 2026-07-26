package nl.madebypatrick.flipiq.ui.scan

/**
 * One line of recognised text plus how tall it was in the frame.
 *
 * The height is the whole point: on a game/DVD/book cover the product name is the *biggest* text,
 * whereas the *longest* string is almost always a marketing blurb, a platform banner or publisher
 * legalese. Kept free of Android/ML Kit types so [bestTitleGuess] is a plain unit test.
 */
data class OcrLine(val text: String, val height: Int)

/** Lines that appear on nearly every cover and are hardly ever the product name. */
private val Boilerplate = listOf(
    "playstation", "nintendo", "xbox", "blu-ray", "bluray", "dvd video", "pegi",
    "only on", "exclusive", "all rights reserved", "www.", "made in", "©",
)

private val WhitespaceRun = Regex("\\s+")

/** Lines within this fraction of the tallest line's height are treated as part of the same title. */
private const val SAME_SIZE_FLOOR = 0.75

/**
 * Best guess at the product name from a single OCR frame.
 *
 * Takes the tallest usable line and glues on any other line within [SAME_SIZE_FLOOR] of that height
 * (so multi-line titles stay together), in reading order.
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
