package nl.madebypatrick.flipiq.data.resolver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * UPCitemdb free "trial" lookup (keyless, rate-limited). General-merchandise barcode → product name.
 * Endpoint: https://api.upcitemdb.com/prod/trial/lookup?upc=<code>
 */
interface UpcItemDbApi {
    @GET("prod/trial/lookup")
    suspend fun lookup(@Query("upc") upc: String): UpcItemDbResponse
}

@Serializable
data class UpcItemDbResponse(
    @SerialName("code") val code: String? = null,
    @SerialName("items") val items: List<UpcItem> = emptyList(),
)

@Serializable
data class UpcItem(
    @SerialName("title") val title: String? = null,
    @SerialName("brand") val brand: String? = null,
)

/** First non-blank item title, or null. Pure so it can be unit-tested against sample payloads. */
fun UpcItemDbResponse.firstTitle(): String? =
    if (code == "OK") items.firstNotNullOfOrNull { it.title?.takeIf(String::isNotBlank) } else null

class UpcItemDbResolver(
    private val api: UpcItemDbApi,
) : BarcodeResolver {
    override suspend fun resolveTitle(barcode: String): String? =
        runCatching { api.lookup(barcode).firstTitle() }.getOrNull()
}
