package nl.madebypatrick.flipiq.data.source.ebay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * eBay OAuth (client-credentials) + Browse API.
 *
 * Note: the Browse API returns **active** listings, not sold comps — sold prices live behind eBay's
 * gated Marketplace Insights API. So this contributes buy opportunities, not sold history.
 * Uses the EBAY_NL marketplace, so prices come back in EUR (no conversion needed).
 */
interface EbayApi {

    @FormUrlEncoded
    @POST("identity/v1/oauth2/token")
    suspend fun token(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("scope") scope: String = "https://api.ebay.com/oauth/api_scope",
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
