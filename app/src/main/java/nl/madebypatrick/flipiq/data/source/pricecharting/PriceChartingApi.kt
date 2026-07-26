package nl.madebypatrick.flipiq.data.source.pricecharting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * PriceCharting product API (https://www.pricecharting.com/api-documentation).
 * Looks a product up by its UPC/EAN — exactly what a barcode scan gives us — or by name, which is
 * all a front/OCR scan can offer.
 */
interface PriceChartingApi {

    @GET("api/product")
    suspend fun productByUpc(
        @Query("t") token: String,
        @Query("upc") upc: String,
    ): PriceChartingProductDto

    /**
     * Full-text lookup by product name (and optionally console), returning the single best match.
     * Used for front/OCR scans, where there is no barcode at all.
     */
    @GET("api/product")
    suspend fun productByName(
        @Query("t") token: String,
        @Query("q") query: String,
    ): PriceChartingProductDto
}

/**
 * PriceCharting returns prices as integer US cents. Any of the price fields may be absent or 0 when
 * PriceCharting has no data for that variant.
 */
@Serializable
data class PriceChartingProductDto(
    @SerialName("status") val status: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("product-name") val productName: String? = null,
    @SerialName("console-name") val consoleName: String? = null,
    @SerialName("loose-price") val loosePrice: Long? = null,
    @SerialName("cib-price") val cibPrice: Long? = null,
    @SerialName("new-price") val newPrice: Long? = null,
    @SerialName("upc") val upc: String? = null,
)
