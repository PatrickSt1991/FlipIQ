package nl.madebypatrick.flipiq.data.source.reway

import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money
import java.util.concurrent.ConcurrentHashMap

/**
 * Reway as a marketplace source. The one class is constructed twice with different hosts/ids (§6):
 *
 *  - `reway_buyin` — `rewayverkopen.nl`, a guaranteed **buy-in** (what Reway pays you), tagged
 *    [ListingType.TRADE_IN] so it never enters the resale median (§3);
 *  - `reway_retail` — `reway.nl`, a real NL **asking** price, tagged [ListingType.ACTIVE].
 *
 * Contract (§6): never throws (`runCatching` → `available = false`); prices are already EUR so **no
 * CurrencyConverter**; emits **at most one** listing (the best match); `shortcutUrl` is the cleaned
 * absolute product URL, falling back to a search URL.
 *
 * Rate limiting for Haul: the shared [RewayThrottle] caps concurrency and honours 429 `Retry-After`;
 * results are cached by (normalised title, platform) for ~1 day (§6 — Reway bulk-reprices daily, so
 * hourly freshness is pointless); the retail store opts out of Haul via [skipDuringHaul].
 */
class RewaySource(
    private val api: RewayApi,
    private val throttle: RewayThrottle,
    override val id: String,
    override val displayName: String,
    private val host: String,
    private val listingType: ListingType,
    private val searchUrl: (String) -> String,
    /** Retail sets this true: during a bulk Haul the buy-in floor is all triage needs (§6). */
    private val skipDuringHaul: Boolean = false,
) : MarketplaceSource {

    private data class CacheKey(val title: String, val platform: String)
    private data class CacheEntry(val result: SourceResult, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()

    override suspend fun lookup(query: ProductQuery): SourceResult {
        val title = query.title?.trim().orEmpty()
        if (title.isBlank()) return unavailable()
        if (query.haul && skipDuringHaul) return unavailable()

        // §4: Reway is unusable without a platform — a wrong one costs 17–23×. Fall back to any
        // platform embedded in the title (OCR scans), else report unavailable rather than guess.
        val platform = query.platform ?: RewayMatching.platformInText(title) ?: return unavailable()
        // Default to games: this is a game-flipping app, and it keeps LEGO/toy false-positives out.
        val category = query.category ?: DEFAULT_CATEGORY

        val key = CacheKey(
            title = RewayMatching.normalise(title).joinToString(" "),
            platform = RewayMatching.canonicalPlatform(platform) ?: platform.lowercase(),
        )
        cacheGet(key)?.let { return it }

        // Circuit-breaker tripped by an earlier 429 — stop querying Reway, don't cache the skip.
        if (throttle.isBlocked()) return unavailable()

        return runCatching {
            val products = throttle.withPermit { api.suggest(url = suggestUrl, q = title) }.products
            val result = RewayMatching.bestMatch(title, platform, category, products)
                ?.let { toSourceResult(it) }
                ?: unavailable()
            // Cache genuine responses (a hit or a clean "no match"); network failures fall through.
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
            result
        }.getOrElse { unavailable() }
    }

    private fun cacheGet(key: CacheKey): SourceResult? =
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.result

    private val suggestUrl get() = host.trimEnd('/') + "/search/suggest.json"

    private fun unavailable() =
        SourceResult(sourceId = id, listings = emptyList(), available = false, shortcutUrl = null)

    /** Map the winning product into a one-listing result. */
    private fun toSourceResult(product: RewayProductDto): SourceResult {
        val cents = RewayMatching.parsePriceCents(product.priceMax ?: product.price)
            ?: return unavailable()
        val cleanUrl = cleanProductUrl(product.url)
        val listing = MarketListing(
            sourceId = id,
            title = product.title ?: displayName,
            price = Money.ofCents(cents),
            type = listingType,
            condition = conditionFromTags(product.tags),
            url = cleanUrl,
        )
        return SourceResult(
            sourceId = id,
            listings = listOf(listing),
            productTitle = product.title,
            category = product.category,
            available = true,
            shortcutUrl = cleanUrl ?: searchUrl(product.title ?: ""),
        )
    }

    /** Strip `_`-prefixed tracking params (`_pos`, `_psq`, …) and prefix the host (§2). */
    private fun cleanProductUrl(raw: String?): String? {
        val url = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val path = url.substringBefore('?')
        val query = url.substringAfter('?', "")
        val keptQuery = query.split('&')
            .filter { it.isNotBlank() && !it.substringBefore('=').startsWith("_") }
            .joinToString("&")
        val absolute = when {
            path.startsWith("http") -> path
            path.startsWith("/") -> host.trimEnd('/') + path
            else -> host.trimEnd('/') + "/" + path
        }
        return if (keptQuery.isBlank()) absolute else "$absolute?$keptQuery"
    }

    private fun conditionFromTags(tags: List<String>): Condition? = when {
        tags.any { it.equals("conditie-nieuw", ignoreCase = true) } -> Condition.SEALED
        tags.any { it.equals("conditie-tweedehands", ignoreCase = true) } -> Condition.GOOD
        else -> null
    }

    companion object {
        const val BUY_IN_ID = "reway_buyin"
        const val RETAIL_ID = "reway_retail"
        const val BUY_IN_HOST = "https://www.rewayverkopen.nl"
        const val RETAIL_HOST = "https://www.reway.nl"
        private const val DEFAULT_CATEGORY = "Games"
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000 // ~1 day; Reway bulk-reprices daily.
    }
}
