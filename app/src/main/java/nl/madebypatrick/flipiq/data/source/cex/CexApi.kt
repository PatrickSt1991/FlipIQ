package nl.madebypatrick.flipiq.data.source.cex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * CeX (webuy) public search API. Keyless — no token or OAuth needed. Prices are in **GBP**.
 * Endpoint: https://wss2.cex.uk.webuy.io/v3/boxes?q=<query>
 */
interface CexApi {

    @GET("v3/boxes")
    suspend fun search(@Query("q") query: String): CexResponse
}

@Serializable
data class CexResponse(
    @SerialName("response") val response: CexResponseBody? = null,
)

@Serializable
data class CexResponseBody(
    @SerialName("ack") val ack: String? = null,
    @SerialName("data") val data: CexData? = null,
)

@Serializable
data class CexData(
    @SerialName("boxes") val boxes: List<CexBox> = emptyList(),
)

@Serializable
data class CexBox(
    @SerialName("boxId") val boxId: String? = null,
    @SerialName("boxName") val boxName: String? = null,
    @SerialName("categoryName") val categoryName: String? = null,
    /** Price to buy the item from CeX (a real acquisition option). */
    @SerialName("sellPrice") val sellPrice: Double? = null,
    /** Cash CeX pays you for it. */
    @SerialName("cashPrice") val cashPrice: Double? = null,
    /** Voucher/exchange value CeX gives you. */
    @SerialName("exchangePrice") val exchangePrice: Double? = null,
)
