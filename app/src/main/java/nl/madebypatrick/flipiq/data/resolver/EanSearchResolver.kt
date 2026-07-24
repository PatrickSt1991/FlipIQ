package nl.madebypatrick.flipiq.data.resolver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * EAN-Search.org barcode lookup — strong EU/EAN coverage (better than UPCitemdb for Dutch products).
 * Needs an API token. Endpoint: https://api.ean-search.org/api?op=barcode-lookup&format=json&ean=...
 */
interface EanSearchApi {
    @GET("api")
    suspend fun lookup(
        @Query("token") token: String,
        @Query("ean") ean: String,
        @Query("op") op: String = "barcode-lookup",
        @Query("format") format: String = "json",
    ): List<EanSearchItem>
}

@Serializable
data class EanSearchItem(
    @SerialName("ean") val ean: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("error") val error: String? = null,
)

/** First real product name, or null. Pure so it's unit-testable against sample payloads. */
fun List<EanSearchItem>.firstTitle(): String? =
    firstOrNull { it.error == null }?.name?.takeIf(String::isNotBlank)

class EanSearchResolver(
    private val api: EanSearchApi,
    private val tokenProvider: suspend () -> String,
) : BarcodeResolver {
    override suspend fun resolveTitle(barcode: String): String? {
        val token = runCatching { tokenProvider() }.getOrDefault("")
        if (token.isBlank()) return null
        return runCatching { api.lookup(token, barcode).firstTitle() }.getOrNull()
    }
}
