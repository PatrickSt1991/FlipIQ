package nl.madebypatrick.flipiq.data.source.mock

import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money
import kotlin.math.abs
import kotlin.random.Random

/** A base product profile the mock sources generate realistic listings from. */
data class MockProduct(
    val barcode: String,
    val title: String,
    val category: String,
    val imageUrl: String?,
    /** Reference market value in cents (roughly the median sold price). */
    val baseMedianCents: Long,
    /** How many recent sold listings exist across marketplaces (liquidity). */
    val soldVolume: Int,
    /** Price spread as a fraction of the median (0.15 = ±15%). */
    val volatility: Double,
)

/**
 * Fixture data source. A handful of hand-authored products (including the README's LEGO Jurassic
 * World example) plus a deterministic fallback so *any* scanned barcode returns something coherent —
 * ideal for developing and demoing the app before real marketplace APIs are wired in.
 *
 * Everything is seeded off the barcode, so the same scan always yields the same numbers.
 */
object MockCatalog {

    private val known: Map<String, MockProduct> = listOf(
        MockProduct(
            barcode = "5051888223451",
            title = "LEGO Jurassic World (PS4)",
            category = "Video Games",
            imageUrl = null,
            baseMedianCents = 1150,
            soldVolume = 14,
            volatility = 0.12,
        ),
        MockProduct(
            barcode = "0711719417972",
            title = "The Last of Us Part II (PS4)",
            category = "Video Games",
            imageUrl = null,
            baseMedianCents = 1995,
            soldVolume = 22,
            volatility = 0.18,
        ),
        MockProduct(
            barcode = "5702016367447",
            title = "LEGO Technic Bugatti Chiron (42083)",
            category = "LEGO",
            imageUrl = null,
            baseMedianCents = 29900,
            soldVolume = 9,
            volatility = 0.20,
        ),
        MockProduct(
            barcode = "9780545010221",
            title = "Harry Potter and the Deathly Hallows (Hardcover)",
            category = "Books",
            imageUrl = null,
            baseMedianCents = 850,
            soldVolume = 5,
            volatility = 0.30,
        ),
    ).associateBy { it.barcode }

    /** Resolve a product, synthesising a deterministic one for unknown barcodes. */
    fun productFor(barcode: String): MockProduct =
        known[barcode] ?: synthesise(barcode)

    private fun synthesise(barcode: String): MockProduct {
        val rng = seededRandom(barcode, salt = "product")
        val median = (500 + rng.nextInt(0, 8000)).toLong() // €5–€85
        val categories = listOf("Video Games", "DVDs", "Blu-rays", "Electronics", "Collectibles", "Books")
        return MockProduct(
            barcode = barcode,
            title = "Scanned item #${barcode.takeLast(5)}",
            category = categories[abs(barcode.hashCode()) % categories.size],
            imageUrl = null,
            baseMedianCents = median,
            soldVolume = 3 + rng.nextInt(0, 20),
            volatility = 0.10 + rng.nextDouble() * 0.25,
        )
    }

    /**
     * Generate up to [max] SOLD listings for a product from a given source, deterministically.
     * Prices are drawn around the median within the volatility band; sale dates are spread across
     * the last ~90 days so sell-speed and trend analysis have something to chew on.
     */
    fun soldListings(product: MockProduct, sourceId: String, max: Int): List<MarketListing> {
        val rng = seededRandom(product.barcode, salt = "sold-$sourceId")
        val count = minOf(max, product.soldVolume)
        return List(count) { i ->
            val jitter = 1.0 + (rng.nextDouble() * 2 - 1) * product.volatility
            val price = (product.baseMedianCents * jitter).toLong().coerceAtLeast(100)
            MarketListing(
                sourceId = sourceId,
                title = product.title,
                price = Money.ofCents(price),
                type = ListingType.SOLD,
                daysAgo = 1 + rng.nextInt(0, 90),
            )
        }.sortedByDescending { it.daysAgo }
    }

    /**
     * Generate ACTIVE (currently-buyable) listings, typically a little below market — these are the
     * buy opportunities the engine scores deals against.
     */
    fun activeListings(product: MockProduct, sourceId: String, count: Int, discount: Double): List<MarketListing> {
        val rng = seededRandom(product.barcode, salt = "active-$sourceId")
        return List(count) {
            val jitter = 1.0 + (rng.nextDouble() * 2 - 1) * (product.volatility / 2)
            val price = (product.baseMedianCents * (1 - discount) * jitter).toLong().coerceAtLeast(100)
            MarketListing(
                sourceId = sourceId,
                title = product.title,
                price = Money.ofCents(price),
                type = ListingType.ACTIVE,
            )
        }
    }

    private fun seededRandom(barcode: String, salt: String): Random =
        Random(barcode.hashCode().toLong() * 31 + salt.hashCode())
}
