package nl.madebypatrick.flipiq.data.source.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * FlipIQ Engine — our own aggregator service (a Cloudflare Worker) that returns marketplace pricing
 * the app can't get directly (currently Marktplaats). Keeps scraper details off the device.
 * See github.com/PatrickSt1991/flipiq-engine.
 */
interface EngineApi {
    /** @Url is the full "…/prices" endpoint so the base URL can be configured at runtime/build. */
    @GET
    suspend fun prices(
        @Url url: String,
        @Header("X-App-Key") appKey: String,
        @Query("q") query: String,
        @Query("ean") ean: String?,
    ): EngineResponse
}

@Serializable
data class EngineResponse(
    @SerialName("listings") val listings: List<EngineListing> = emptyList(),
    @SerialName("providers") val providers: List<EngineProvider> = emptyList(),
)

@Serializable
data class EngineListing(
    @SerialName("source") val source: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("price_cents") val priceCents: Long? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("type") val type: String? = null,   // "ASKING" | "SOLD"
    @SerialName("url") val url: String? = null,
)

@Serializable
data class EngineProvider(
    @SerialName("provider") val provider: String? = null,
    @SerialName("available") val available: Boolean = false,
    @SerialName("count") val count: Int = 0,
)
