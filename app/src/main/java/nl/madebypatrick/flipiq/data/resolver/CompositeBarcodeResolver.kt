package nl.madebypatrick.flipiq.data.resolver

/**
 * Tries each resolver in order and returns the first non-blank title. Lets us prefer a stronger
 * (token'd) source like EAN-Search and fall back to the keyless UPCitemdb.
 */
class CompositeBarcodeResolver(
    private val resolvers: List<BarcodeResolver>,
) : BarcodeResolver {
    override suspend fun resolveTitle(barcode: String): String? {
        for (resolver in resolvers) {
            val title = runCatching { resolver.resolveTitle(barcode) }.getOrNull()
            if (!title.isNullOrBlank()) return title
        }
        return null
    }
}
