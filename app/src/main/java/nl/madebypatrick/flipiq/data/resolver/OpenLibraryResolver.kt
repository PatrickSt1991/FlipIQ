package nl.madebypatrick.flipiq.data.resolver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * OpenLibrary ISBN lookup — keyless and authoritative for books. Book barcodes are EAN-13 ISBNs
 * (Bookland, prefix 978/979). Endpoint: https://openlibrary.org/isbn/<isbn>.json
 */
interface OpenLibraryApi {
    @GET("isbn/{isbn}.json")
    suspend fun book(@Path("isbn") isbn: String): OpenLibraryBook
}

@Serializable
data class OpenLibraryBook(
    @SerialName("title") val title: String? = null,
    @SerialName("full_title") val fullTitle: String? = null,
)

class OpenLibraryResolver(
    private val api: OpenLibraryApi,
) : BarcodeResolver {
    override suspend fun resolveTitle(barcode: String): String? {
        if (!isIsbn(barcode)) return null
        val book = runCatching { api.book(barcode) }.getOrNull() ?: return null
        return (book.fullTitle ?: book.title)?.takeIf(String::isNotBlank)
    }

    /** Book barcodes are 13-digit EANs starting 978 or 979 (Bookland). */
    private fun isIsbn(barcode: String): Boolean =
        barcode.length == 13 && (barcode.startsWith("978") || barcode.startsWith("979"))
}
