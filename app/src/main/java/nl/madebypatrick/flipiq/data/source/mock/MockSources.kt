package nl.madebypatrick.flipiq.data.source.mock

import kotlinx.coroutines.delay
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.MarketplaceUrls
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult

/**
 * Mock marketplace sources backed by [MockCatalog], used **only in demo mode** (debug builds) so the
 * app is explorable with realistic data. Release builds feed the engine from real sources only.
 *
 * The small [delay] on each simulates network latency so loading states are visible while developing.
 */

/** eBay — the deepest source of *sold* comps, plus a couple of active "buy it now" listings. */
class EbaySoldSource : MarketplaceSource {
    override val id = "ebay"
    override val displayName = "eBay Sold"
    override suspend fun lookup(query: ProductQuery): SourceResult {
        delay(220)
        val product = MockCatalog.productFor(query.barcode)
        return SourceResult(
            sourceId = id,
            listings = MockCatalog.soldListings(product, id, max = 20) +
                MockCatalog.activeListings(product, id, count = 2, discount = 0.25),
            productTitle = product.title,
            category = product.category,
            imageUrl = product.imageUrl,
            shortcutUrl = MarketplaceUrls.ebaySold(product.title),
        )
    }
}
