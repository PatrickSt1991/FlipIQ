package nl.madebypatrick.flipiq.data.source.mock

import kotlinx.coroutines.delay
import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.MarketplaceUrls
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult

/**
 * Mock marketplace sources backed by [MockCatalog]. Each mimics the character of the real
 * marketplace it stands in for. Swapping any one for a real API/scraper is a drop-in replacement —
 * nothing downstream cares where the listings came from.
 *
 * The small [delay] on each simulates network latency so loading states are visible while developing.
 */

/** eBay — the deepest source of *sold* comps. */
class EbaySoldSource : MarketplaceSource {
    override val id = "ebay"
    override val displayName = "eBay Sold"
    override suspend fun lookup(query: ProductQuery): SourceResult {
        delay(220)
        val product = MockCatalog.productFor(query.barcode)
        return SourceResult(
            sourceId = id,
            listings = MockCatalog.soldListings(product, id, max = 20),
            productTitle = product.title,
            category = product.category,
            imageUrl = product.imageUrl,
            shortcutUrl = MarketplaceUrls.ebaySold(product.title),
        )
    }
}

/** PriceCharting — curated sold prices, strongest for games and collectibles. */
class PriceChartingSource : MarketplaceSource {
    override val id = "pricecharting"
    override val displayName = "PriceCharting"
    override suspend fun lookup(query: ProductQuery): SourceResult {
        delay(180)
        val product = MockCatalog.productFor(query.barcode)
        val relevant = product.category in setOf("Video Games", "LEGO", "Collectibles")
        return SourceResult(
            sourceId = id,
            listings = if (relevant) MockCatalog.soldListings(product, id, max = 8) else emptyList(),
            productTitle = product.title,
            category = product.category,
            available = relevant,
            shortcutUrl = MarketplaceUrls.priceCharting(product.title),
        )
    }
}

/** CeX — a guaranteed buy price (their cage price), modelled as one active/buyable listing. */
class CexSource : MarketplaceSource {
    override val id = "cex"
    override val displayName = "CeX"
    override suspend fun lookup(query: ProductQuery): SourceResult {
        delay(150)
        val product = MockCatalog.productFor(query.barcode)
        return SourceResult(
            sourceId = id,
            listings = MockCatalog.activeListings(product, id, count = 1, discount = 0.10),
            productTitle = product.title,
            shortcutUrl = MarketplaceUrls.cex(product.title),
        )
    }
}

/** Vinted — active peer listings, often the cheapest buy opportunities. */
class VintedSource : MarketplaceSource {
    override val id = "vinted"
    override val displayName = "Vinted"
    override suspend fun lookup(query: ProductQuery): SourceResult {
        delay(200)
        val product = MockCatalog.productFor(query.barcode)
        return SourceResult(
            sourceId = id,
            listings = MockCatalog.activeListings(product, id, count = 3, discount = 0.30),
            productTitle = product.title,
            shortcutUrl = MarketplaceUrls.vinted(product.title),
        )
    }
}

/** Marktplaats — active local listings. */
class MarktplaatsSource : MarketplaceSource {
    override val id = "marktplaats"
    override val displayName = "Marktplaats"
    override suspend fun lookup(query: ProductQuery): SourceResult {
        delay(200)
        val product = MockCatalog.productFor(query.barcode)
        return SourceResult(
            sourceId = id,
            listings = MockCatalog.activeListings(product, id, count = 2, discount = 0.25),
            productTitle = product.title,
            shortcutUrl = MarketplaceUrls.marktplaats(product.title),
        )
    }
}
