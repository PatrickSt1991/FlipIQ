package nl.madebypatrick.flipiq.data.source.ebay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * eBay Browse API + a token fetched from our own **proxy** (a Cloudflare Worker that holds the
 * client id/secret server-side). The app never ships the eBay secret.
 *
 * Note: Browse returns **active** listings, not sold comps (sold prices need eBay's gated
 * Marketplace Insights API). Uses the EBAY_NL marketplace, so prices come back in EUR.
 */
interface EbayApi {

    /** Fetch a short-lived application token from the proxy (full URL via @Url). */
    @GET
    suspend fun proxyToken(
        @Url proxyUrl: String,
        @Header("X-App-Key") appKey: String,
    ): EbayTokenResponse

    @GET("buy/browse/v1/item_summary/search")
    suspend fun search(
        @Header("Authorization") bearerAuth: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Header("X-EBAY-C-MARKETPLACE-ID") marketplace: String = "EBAY_NL",
    ): EbaySearchResponse
}

@Serializable
data class EbayTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0,
)

@Serializable
data class EbaySearchResponse(
    @SerialName("itemSummaries") val itemSummaries: List<EbayItemSummary> = emptyList(),
)

@Serializable
data class EbayItemSummary(
    @SerialName("title") val title: String? = null,
    @SerialName("itemWebUrl") val itemWebUrl: String? = null,
    @SerialName("price") val price: EbayPrice? = null,
    @SerialName("condition") val condition: String? = null,
)

@Serializable
data class EbayPrice(
    @SerialName("value") val value: String? = null,
    @SerialName("currency") val currency: String? = null,
)
