package nl.madebypatrick.flipiq.ui.share

/**
 * Recover a searchable game title (or a bare barcode) from whatever text an app puts on the
 * clipboard when you hit *Share*. Deliberately free of Android types so it unit-tests as plain JVM,
 * the same way [nl.madebypatrick.flipiq.ui.scan.bestTitleGuess] does. `SharedTextTest` is the spec:
 * if an app changes its wording, add the new string to that test first.
 */

private val Whitespace = Regex("\\s+")
private val UrlRegex = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

// Slug word separators. Whitespace lives *inside* the class so this resolves to split(Regex), not
// the split(Regex, limit) overload.
private val SlugSeparators = Regex("[-_+\\s]")

// An id-like token: an optional single leading letter then digits (m2145678901, 42083, 183456789012).
private val IdLike = Regex("^[a-z]?\\d+$")

// Straight and curly quote characters, in one class for both open and close.
private val Quoted = Regex("[\"'“”‘’]([^\"'“”‘’]{2,})[\"'“”‘’]")

// Category/browse/search path segments — their slugs are category names, not listing titles.
private val BrowseSegments = setOf("l", "c", "q", "b", "sch", "search", "browse", "catalog", "cat")

// A trailing price like "- € 249,00" or "€ 12.50".
private val TrailingPrice = Regex("[\\-–—]?\\s*[€$£]\\s*\\d[\\d.,]*\\s*$")

// A page-title site suffix like "| Marktplaats" or "- 2dehands".
private val SiteSuffix = Regex(
    "\\s*[|\\-–—]\\s*(marktplaats|2dehands|vinted|ebay)(\\.nl)?\\s*$",
    RegexOption.IGNORE_CASE,
)

// Stray edge punctuation to trim off the final result. Plain char set for trim {}, not a regex, so
// nothing is escaped here.
private const val EDGE_PUNCT = "!?.,:;-–—|\"'“”‘’ "

// Share-filler phrases (EN + NL), matched anywhere in a line (apps put filler and title on one
// line), case-insensitive, each swallowing the separators that follow it.
private val FillerPhrases: List<Regex> = listOf(
    "check\\s+(it\\s+|this\\s+)?out(\\s+this\\s+item)?(\\s+i\\s+found)?",
    "(take|have)\\s+a\\s+look\\s+at(\\s+this)?",
    "i\\s+(found|spotted|saw)\\s+this",
    "look\\s+what\\s+i\\s+found",
    "bekijk(\\s+(deze|dit))?(\\s+(advertentie|item|aanbieding|product))?",
    "gevonden\\s+op",
    "te\\s+koop(\\s+op)?",
    "zie(\\s+deze)?\\s+advertentie",
    "op\\s+(marktplaats|vinted|ebay)",
    "on\\s+(ebay|vinted)",
    "(download|get)(\\s+the)?\\s+\\S+\\s+app",
    "(the|de)\\s+\\S+\\s+app",
    "sent\\s+from\\s+my\\s+\\S+",
    "shared\\s+(from|via)",
    "via\\s+\\w+",
    "for\\s+sale",
    "advertentie",
).map { Regex("$it[\\s:,.\\-–—]*", RegexOption.IGNORE_CASE) }

/**
 * The whole shared message is a single UPC/EAN and nothing else → return it. 8..14 digits after a
 * trim; any surrounding text (a URL, a label like "EAN") disqualifies it.
 */
fun sharedBarcode(raw: String): String? {
    val trimmed = raw.trim()
    return if (trimmed.matches(Regex("\\d{8,14}"))) trimmed else null
}

/**
 * Best-effort listing title from shared text. Builds three candidates — quoted text, a URL slug and
 * leftover prose — and picks between them (see the class doc and `SharedTextTest`). Returns null when
 * nothing usable survives.
 */
fun sharedTitle(raw: String, maxLength: Int = 80): String? {
    // 1. Quoted text — when an app quotes the name it's exactly right.
    quotedCandidate(raw)?.let { clean(it, maxLength) }?.takeIf(::valid)?.let { return it }

    val slug = slugCandidate(raw)?.let { clean(it, maxLength) }?.takeIf(::valid)
    val prose = proseCandidate(raw)?.let { clean(it, maxLength) }?.takeIf(::valid)

    return when {
        slug != null && prose != null ->
            // Cross-check rather than rank blindly: a shared word means the prose is really the same
            // title (keep its casing/punctuation); nothing shared means the prose is app chatter and
            // the slug is the structural title.
            if (shareSignificantWord(slug, prose)) prose else slug
        prose != null -> prose
        else -> slug
    }
}

private fun quotedCandidate(raw: String): String? = Quoted.find(raw)?.groupValues?.get(1)

private fun slugCandidate(raw: String): String? {
    val url = UrlRegex.find(raw)?.value ?: return null
    val afterHost = url.substringAfter("://", url).substringAfter('/', "")
    val path = afterHost.substringBefore('?').substringBefore('#')
    val segments = path.split('/').filter { it.isNotBlank() }
    if (segments.isEmpty()) return null
    // A browse/search segment anywhere means the slug is a category, not a listing.
    if (segments.any { it.lowercase() in BrowseSegments }) return null

    // Walk backwards: the last segment needs ≥ 2 real words, an earlier one ≥ 3 (a 2-word earlier
    // segment is usually a category).
    for (i in segments.indices.reversed()) {
        val words = segments[i]
            .split(SlugSeparators)
            .filter { it.isNotBlank() && !IdLike.matches(it.lowercase()) }
        val need = if (i == segments.lastIndex) 2 else 3
        if (words.size >= need) return words.joinToString(" ")
    }
    return null
}

private fun proseCandidate(raw: String): String? =
    raw.lines()
        .map { stripFiller(UrlRegex.replace(it, " ")) }
        .maxByOrNull { it.trim().length }
        ?.takeIf { it.isNotBlank() }

private fun stripFiller(line: String): String {
    var text = line
    for (phrase in FillerPhrases) text = phrase.replace(text, " ")
    return text
}

/** True when the two strings share a ≥ 4-char word (case-insensitive). */
private fun shareSignificantWord(a: String, b: String): Boolean {
    fun words(s: String) =
        s.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 4 }.toSet()
    val wordsB = words(b)
    return words(a).any { it in wordsB }
}

private fun clean(raw: String, maxLength: Int): String {
    var text = raw.replace(Whitespace, " ").trim()
    // Marktplaats/eBay share text is "Title | Platform | Condition" (or a page title "Title | eBay").
    // The first pipe segment is the listing title — keep it, drop the rest.
    if ('|' in text) {
        text.split('|').firstOrNull { it.count(Char::isLetter) >= 2 }?.let { text = it.trim() }
    }
    text = TrailingPrice.replace(text, "").trim()
    text = SiteSuffix.replace(text, "").trim()
    text = text.trim { it in EDGE_PUNCT }
    return text.take(maxLength).trim()
}

/** Same bar as `bestTitleGuess`: needs at least two letters to be a plausible title. */
private fun valid(s: String): Boolean = s.count(Char::isLetter) >= 2
