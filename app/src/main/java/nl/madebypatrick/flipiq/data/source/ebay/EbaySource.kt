package nl.madebypatrick.flipiq.data.source.ebay

import android.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money

/**
 * Caches an eBay OAuth token (client-credentials) until shortly before it expires, refreshing on
 * demand. Thread-safe via a mutex.
 */
class EbayAuthenticator(
    private val api: EbayApi,
    private val clientId: String,
    private val clientSecret: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var cachedToken: String? = null
    private var expiresAtMs: Long = 0

    val hasCredentials: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    /** Returns a valid bearer token, or null if credentials are missing / the token call fails. */
    suspend fun bearer(): String? {
        if (!hasCredentials) return null
        mutex.withLock {
            if (cachedToken != null && now() < expiresAtMs) return "Bearer $cachedToken"
            val basic = "Basic " + Base64.encodeToString(
                "$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP,
            )
            val resp = runCatching { api.token(basic) }.getOrNull() ?: return null
            val token = resp.accessToken ?: return null
            cachedToken = token
            // Refresh a minute early to avoid edge-of-expiry failures.
            expiresAtMs = now() + (resp.expiresIn - 60).coerceAtLeast(0) * 1000
            return "Bearer $token"
        }
    }
}

/**
 * Live eBay source (Browse API). Contributes **active** listings as buy opportunities. Wired in only
 * when credentials are configured; network/parse failures degrade to an unavailable, empty result.
 */
class EbaySource(
    private val api: EbayApi,
    private val authenticator: EbayAuthenticator,
) : MarketplaceSource {

    override val id = SOURCE_ID
    override val displayName = "eBay"

    override suspend fun lookup(query: ProductQuery): SourceResult {
        if (!authenticator.hasCredentials) return unavailable()
        val bearer = authenticator.bearer() ?: return unavailable()
        val term = query.title ?: query.barcode
        return runCatching { api.search(bearer, term).toSourceResult() }.getOrElse { unavailable() }
    }

    private fun unavailable() =
        SourceResult(sourceId = id, listings = emptyList(), available = false, shortcutUrl = null)

    companion object {
        const val SOURCE_ID = "ebay"
    }
}

/**
 * Map an eBay Browse response into a [SourceResult] of ACTIVE listings (EUR, from the EBAY_NL
 * marketplace). Pure and side-effect-free so it can be unit-tested against sample payloads.
 */
fun EbaySearchResponse.toSourceResult(): SourceResult {
    val listings = itemSummaries.mapNotNull { item ->
        val cents = item.price?.value?.toDoubleOrNull() ?: return@mapNotNull null
        MarketListing(
            sourceId = EbaySource.SOURCE_ID,
            title = item.title ?: "eBay item",
            price = Money.ofEuros(cents),
            type = ListingType.ACTIVE,
        )
    }
    return SourceResult(
        sourceId = EbaySource.SOURCE_ID,
        listings = listings,
        productTitle = itemSummaries.firstNotNullOfOrNull { it.title },
        available = listings.isNotEmpty(),
        shortcutUrl = itemSummaries.firstNotNullOfOrNull { it.itemWebUrl },
    )
}
