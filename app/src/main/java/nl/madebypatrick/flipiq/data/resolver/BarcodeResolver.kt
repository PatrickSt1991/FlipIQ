package nl.madebypatrick.flipiq.data.resolver

/**
 * Resolves a scanned barcode (UPC/EAN) to a human product name. A good title lets name-based
 * marketplaces (CeX, eBay, the shortcut links) match far better than a raw barcode.
 *
 * Pluggable, like the marketplace sources — swap the implementation without touching callers.
 */
interface BarcodeResolver {
    /** Returns a product title, or null if it can't be resolved (never throws). */
    suspend fun resolveTitle(barcode: String): String?
}

/** No-op resolver: used in demo mode, where mock sources already supply titles. */
class NoopBarcodeResolver : BarcodeResolver {
    override suspend fun resolveTitle(barcode: String): String? = null
}
