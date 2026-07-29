package nl.madebypatrick.flipiq.data.source.reway

import java.text.Normalizer

/**
 * Matching Reway's loosely-ranked `suggest.json` hits to a scanned item. **This is the whole feature**
 * (§4): `suggest.json` returns up to ~10 candidates across unrelated categories, and identical titles
 * differ 17–23× in price purely by platform, so picking the wrong row is worse than returning nothing.
 *
 * Pure Kotlin — no Android imports — so it runs as a plain JVM unit test, the same as
 * [nl.madebypatrick.flipiq.ui.scan.TitleGuess] and [nl.madebypatrick.flipiq.ui.share.SharedText].
 */
object RewayMatching {

    // Tokens dropped as noise: platform words (the platform lives in `vendor`, not the title),
    // condition/region qualifiers, and leading articles. NOT "of" — it's load-bearing in
    // "God of War". Kept lower-case; matched after accents/punctuation are stripped.
    private val NOISE = setOf(
        "playstation", "ps", "ps1", "ps2", "ps3", "ps4", "ps5", "psx", "psp", "vita", "psvita",
        "nintendo", "switch", "xbox", "wii", "wiiu", "gamecube", "gc", "ds", "dsi", "3ds", "2ds",
        "nes", "snes", "n64", "gba", "gameboy", "megadrive", "saturn", "dreamcast",
        "game", "games", "spel", "spellen", "pal", "ntsc", "nl", "eu", "uk", "usa",
        "editie", "edition", "version", "versie", "tweedehands", "nieuw", "used", "new",
        "the", "de", "het", "een", "a", "an",
    )

    // Canonical platform ids. Both the query platform and a candidate's `vendor`/tags map through
    // this; only when *both* map to a known-but-different id do we reject on platform (§4: never
    // reject on unknown/missing).
    private val PLATFORM_ALIASES: Map<String, String> = buildMap {
        fun alias(canonical: String, vararg forms: String) = forms.forEach { put(it, canonical) }
        alias("ps1", "playstation", "playstation 1", "playstation one", "ps1", "ps one", "psx")
        alias("ps2", "playstation 2", "ps2")
        alias("ps3", "playstation 3", "ps3")
        alias("ps4", "playstation 4", "ps4")
        alias("ps5", "playstation 5", "ps5")
        alias("psp", "psp", "playstation portable")
        alias("psvita", "ps vita", "psvita", "playstation vita", "vita")
        alias("switch", "switch", "nintendo switch")
        alias("wii", "wii", "nintendo wii")
        alias("wiiu", "wii u", "wiiu")
        alias("gamecube", "gamecube", "game cube", "ngc")
        alias("ds", "ds", "nintendo ds", "ds / dsi", "dsi")
        alias("3ds", "3ds", "nintendo 3ds")
        alias("2ds", "2ds", "nintendo 2ds")
        alias("xboxclassic", "xbox classic", "xbox", "xbox original")
        alias("xbox360", "xbox 360")
        alias("xboxone", "xbox one")
        alias("xboxseries", "xbox series", "xbox series x", "xbox series s")
        alias("lego", "lego")
        alias("vtech", "vtech", "v.smile", "vsmile")
    }

    private const val EXTRA_TOKEN_PENALTY = 0.15
    private const val HIGH_COVERAGE_THRESHOLD = 0.75

    // --- Public helpers (also unit-tested directly) -----------------------------------------

    /**
     * Parse a Reway price into whole cents. `suggest.json`/`products.json` give decimal strings
     * (`"23.95"`, `"0,31"`); `/products/x.js` gives integer cents (`1200`). A string containing `.`
     * or `,` is euros; anything else is already cents. Non-positive → null (`"0.00"` is "no price").
     */
    internal fun parsePriceCents(raw: String?): Long? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        return if (s.contains('.') || s.contains(',')) {
            val euros = s.replace(',', '.').toDoubleOrNull() ?: return null
            Math.round(euros * 100)
        } else {
            s.toLongOrNull()
        }?.takeIf { it > 0 }
    }

    /** Integer-cents overload for `/products/x.js` (`1200`); validates positivity only. */
    internal fun parsePriceCents(cents: Long?): Long? = cents?.takeIf { it > 0 }

    /**
     * Normalise a title into comparable tokens: lower-case, unescape `\uXXXX`, strip accents and all
     * punctuation (hyphens included, so `Spider-man` → `spiderman`), drop a leading Reway set number
     * (`71343 Fun Pack` → `fun pack`), drop noise words and sub-2-char tokens. Bracket qualifiers are
     * *not* here — see [bracketFlags], they're distinct-product flags.
     */
    internal fun normalise(title: String): List<String> {
        val withoutBrackets = title.replace(Regex("""[\[(][^\])]*[\])]"""), " ")
        val cleaned = stripAccents(unescapeUnicode(withoutBrackets).lowercase())
            // Hyphens/apostrophes join words (Spider-man → spiderman); other punctuation splits.
            .replace(Regex("""[-'’]"""), "")
            .replace(Regex("""[^a-z0-9]+"""), " ")
        val tokens = cleaned.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
        // Strip a leading LEGO-style catalogue number (4+ digits at the front), keep sequel numbers.
        val body = if (tokens.firstOrNull()?.let { it.length >= 4 && it.all(Char::isDigit) } == true) {
            tokens.drop(1)
        } else {
            tokens
        }
        // Drop sub-2-char noise, but keep numeric tokens of any length — a bare "2" or "3" is a
        // decisive sequel number, not noise (§4).
        return body.filter { (it.all(Char::isDigit) || it.length >= 2) && it !in NOISE }
    }

    /**
     * Bracketed/parenthesised qualifiers (`[Platinum]`, `[Game Only]`, `(Nieuw)`) normalised to a set.
     * These are distinct products, so they must match if present on either side (§4).
     */
    internal fun bracketFlags(title: String): Set<String> =
        Regex("""[\[(]([^\])]*)[\])]""").findAll(title)
            .map { stripAccents(unescapeUnicode(it.groupValues[1]).lowercase()).replace(Regex("""[^a-z0-9]+"""), " ").trim() }
            .filter { it.isNotBlank() }
            .toSet()

    /** The candidate's platform: `vendor` first, then the most specific known platform in `tags`. */
    internal fun platformOf(product: RewayProductDto): String? {
        product.vendor?.takeIf { it.isNotBlank() }?.let { return it }
        return product.tags.lastOrNull { canonicalPlatform(it) != null }
    }

    /**
     * Best-effort platform extracted from free text (an OCR title like "God of War Playstation 2"),
     * used only when the caller has no explicit platform. Matches known aliases as whole words,
     * longest phrase first, so "wii u" wins over "wii" and "ds" won't fire inside "kids".
     */
    internal fun platformInText(text: String): String? {
        val t = stripAccents(unescapeUnicode(text).lowercase())
        return PLATFORM_ALIASES.keys
            .sortedByDescending { it.length }
            .firstOrNull { key -> Regex("""\b""" + Regex.escape(key) + """\b""").containsMatchIn(t) }
            ?.let { PLATFORM_ALIASES[it] }
    }

    /** Map a free-form platform string to a canonical id, or null when unrecognised. */
    internal fun canonicalPlatform(raw: String?): String? {
        val key = raw?.let { stripAccents(it.lowercase()).replace(Regex("""\s+"""), " ").trim() }
            ?: return null
        return PLATFORM_ALIASES[key]
    }

    /**
     * Pick the single best candidate for [query] on [platform], or null when nothing is a safe match.
     *
     * Rejections run before scoring:
     *  - unavailable, or `price_max` parses to null/0
     *  - a *different known* platform (unknown/missing platforms are never rejected)
     *  - a different [category] (keeps a €384 LEGO set away from a PS4 game scan)
     *  - **platform == null → reject everything** (§4: a wrong platform is worse than no data)
     *  - numeric tokens (sequel numbers) that differ on either side
     *  - bracket flags that differ on either side
     *
     * Scoring: query-token coverage minus a per-extra-candidate-token penalty. Require full coverage
     * unless exactly one candidate clears a high threshold. Ties resolve to the **cheapest**, so the
     * guaranteed floor is never over-promised.
     */
    internal fun bestMatch(
        query: String,
        platform: String?,
        category: String?,
        candidates: List<RewayProductDto>,
    ): RewayProductDto? {
        // §4: without a platform the 17–23× spreads make any pick a coin-flip — report nothing.
        if (platform == null) return null

        val queryTokens = normalise(query)
        if (queryTokens.isEmpty()) return null
        val queryNumbers = queryTokens.filter { it.all(Char::isDigit) }.toSet()
        val queryFlags = bracketFlags(query)
        val wantPlatform = canonicalPlatform(platform)
        val wantCategory = category?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

        val scored = candidates.mapNotNull { c ->
            val title = c.title ?: return@mapNotNull null
            val priceCents = parsePriceCents(c.priceMax ?: c.price) ?: return@mapNotNull null
            if (!c.available) return@mapNotNull null

            // Platform: reject only a *different known* platform.
            val candPlatform = canonicalPlatform(platformOf(c))
            if (wantPlatform != null && candPlatform != null && candPlatform != wantPlatform) {
                return@mapNotNull null
            }
            // Category: reject a different known category.
            if (wantCategory != null && c.category?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
                    ?.let { it != wantCategory } == true
            ) {
                return@mapNotNull null
            }

            val candTokens = normalise(title)
            val candNumbers = candTokens.filter { it.all(Char::isDigit) }.toSet()
            if (candNumbers != queryNumbers) return@mapNotNull null              // sequel numbers decisive
            if (bracketFlags(title) != queryFlags) return@mapNotNull null          // distinct products

            val overlap = queryTokens.count { it in candTokens }
            val coverage = overlap.toDouble() / queryTokens.size
            val extra = (candTokens.toSet() - queryTokens.toSet()).size
            val score = coverage - EXTRA_TOKEN_PENALTY * extra
            Scored(c, priceCents, coverage, score)
        }

        val full = scored.filter { it.coverage >= 1.0 }
        val pool = when {
            full.isNotEmpty() -> full
            else -> scored.filter { it.coverage >= HIGH_COVERAGE_THRESHOLD }.takeIf { it.size == 1 } ?: emptyList()
        }
        if (pool.isEmpty()) return null

        // Best score, ties broken by cheapest, then title for full determinism on duplicates.
        return pool.sortedWith(
            compareByDescending<Scored> { it.score }
                .thenBy { it.priceCents }
                .thenBy { it.product.title ?: "" },
        ).first().product
    }

    private data class Scored(
        val product: RewayProductDto,
        val priceCents: Long,
        val coverage: Double,
        val score: Double,
    )

    private fun unescapeUnicode(s: String): String =
        Regex("""\\u([0-9a-fA-F]{4})""").replace(s) { m -> m.groupValues[1].toInt(16).toChar().toString() }

    private fun stripAccents(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("""\p{M}+"""), "")
}
