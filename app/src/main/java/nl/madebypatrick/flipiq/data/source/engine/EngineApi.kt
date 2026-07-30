package nl.madebypatrick.flipiq.data.source.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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
        @Query("loc") loc: String?,
    ): EngineResponse

    /** Snapshot → game title via the engine's vision model (`/identify`). */
    @POST
    suspend fun identify(
        @Url url: String,
        @Header("X-App-Key") appKey: String,
        @Body body: IdentifyRequest,
    ): IdentifyResponse

    /** Most valuable games for a console (`/top?console=`). */
    @GET
    suspend fun top(
        @Url url: String,
        @Header("X-App-Key") appKey: String,
        @Query("console") console: String,
        @Query("loc") loc: String?,
    ): TopResponse

    /** Many items in one photo → titles with quick resale values (`/haul`). */
    @POST
    suspend fun haul(
        @Url url: String,
        @Header("X-App-Key") appKey: String,
        @Query("loc") loc: String?,
        @Body body: IdentifyRequest,
    ): HaulResponse
}

@Serializable
data class HaulResponse(
    @SerialName("items") val items: List<HaulItemDto> = emptyList(),
)

@Serializable
data class HaulItemDto(
    @SerialName("title") val title: String,
    @SerialName("value_cents") val valueCents: Long? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("image") val image: String? = null,
)

@Serializable
data class TopResponse(
    @SerialName("console") val console: String? = null,
    @SerialName("games") val games: List<TopGame> = emptyList(),
)

@Serializable
data class TopGame(
    @SerialName("title") val title: String,
    @SerialName("price_cents") val priceCents: Long? = null,
    @SerialName("currency") val currency: String? = null,
)

@Serializable
data class IdentifyRequest(
    @SerialName("image") val image: String,           // base64-encoded JPEG
    @SerialName("mime_type") val mimeType: String = "image/jpeg",
)

@Serializable
data class IdentifyResponse(
    @SerialName("title") val title: String? = null,   // null when nothing identifiable
    @SerialName("kind") val kind: String? = null,      // game|music|movie|book|lego|other (routing hint)
)

@Serializable
data class EngineResponse(
    @SerialName("image") val image: String? = null,
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
