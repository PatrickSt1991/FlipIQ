package nl.madebypatrick.flipiq.data.resolver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * eandata.com feed lookup — a large curated barcode DB with good game/media coverage. It **blocks
 * Cloudflare/datacenter IPs**, so this runs **on-device** (the phone's residential IP is fine) as a
 * last-resort resolver for barcodes the engine's keyless sources (ean13/buycott) couldn't resolve.
 * The free keycode has a small daily lookup quota, which is exactly why it's a fallback, not a
 * per-scan source. Endpoint: https://eandata.com/feed/?v=3&keycode=<key>&mode=json&find=<ean>
 */
interface EandataApi {
    @GET("feed/")
    suspend fun lookup(
        @Query("v") v: String = "3",
        @Query("keycode") keycode: String,
        @Query("mode") mode: String = "json",
        @Query("find") find: String,
    ): EandataResponse
}

@Serializable
data class EandataResponse(
    @SerialName("status") val status: EandataStatus? = null,
    @SerialName("product") val product: EandataProduct? = null,
)

@Serializable
data class EandataStatus(@SerialName("code") val code: String? = null)

@Serializable
data class EandataProduct(@SerialName("attributes") val attributes: EandataAttributes? = null)

@Serializable
data class EandataAttributes(@SerialName("product") val product: String? = null)

/** Trailing "(Pc Dvd)"-style format tags are noise for a marketplace search — strip them. */
private val FORMAT_TAG = Regex("""\s*\((pc|dvd|blu-?ray|cd)(\s+(pc|dvd|blu-?ray|cd))*\)\s*$""", RegexOption.IGNORE_CASE)

/** Product name from a successful response, cleaned; null on miss, error, or a Cyrillic name. */
fun EandataResponse.productName(): String? {
    if (status?.code != "200") return null
    val raw = product?.attributes?.product?.trim().orEmpty()
    val name = raw.replace(FORMAT_TAG, "").replace(Regex("\\s+"), " ").trim()
    // Hard guard: eandata is a mixed-language DB — never surface a Cyrillic (Russian/Ukrainian) name.
    if (name.isBlank() || name.any { it in 'Ѐ'..'ӿ' }) return null
    return name
}

class EandataResolver(
    private val api: EandataApi,
    private val keycode: String,
) : BarcodeResolver {
    override suspend fun resolveTitle(barcode: String): String? {
        if (keycode.isBlank()) return null
        return runCatching { api.lookup(keycode = keycode, find = barcode).productName() }.getOrNull()
    }
}
