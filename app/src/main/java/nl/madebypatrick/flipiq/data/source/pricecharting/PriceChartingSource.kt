package nl.madebypatrick.flipiq.data.source.pricecharting

import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.MarketplaceUrls
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.domain.CurrencyConverter
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.Currency
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money

/**
 * Real PriceCharting integration — the first live marketplace source, sitting behind the same
 * [MarketplaceSource] seam as the mocks. It looks a product up by the scanned UPC — or, for a
 * front/OCR scan, by name — and turns PriceCharting's loose/CIB/new price guide into market listings
 * the engine can score.
 *
 * Only wired in when a token is configured (see [nl.madebypatrick.flipiq.di.NetworkModule]); without
 * one the mock stays in place. Guards against network/parse failures by returning an unavailable,
 * empty result rather than throwing.
 *
 * NOTE: PriceCharting prices are in **US dollars**; currency conversion is a follow-up before mixing
 * these with the EUR marketplaces.
 */
class PriceChartingSource(
    private val api: PriceChartingApi,
    /** Resolves the current token (runtime setting, falling back to the build-time value). */
    private val tokenProvider: suspend () -> String,
    private val currencyConverter: CurrencyConverter,
) : MarketplaceSource {

    override val id = SOURCE_ID
    override val displayName = "PriceCharting"

    override suspend fun lookup(query: ProductQuery): SourceResult {
        val token = runCatching { tokenProvider() }.getOrDefault("")
        if (token.isBlank()) return unavailable()
        val title = query.title?.trim().orEmpty()
        if (query.barcode.isBlank() && title.isBlank()) return unavailable()
        return runCatching {
            // A front/OCR scan has no barcode; querying `upc=` with an empty string always misses,
            // which silently knocked PriceCharting out of every title search.
            val product = if (query.barcode.isNotBlank()) {
                api.productByUpc(token, query.barcode)
            } else {
                api.productByName(token, title)
            }
            product
                .toSourceResult()
                .toEur(currencyConverter) // PriceCharting is USD → normalise to the EUR base.
        }.getOrElse { unavailable() }
    }

    private fun unavailable() =
        SourceResult(sourceId = id, listings = emptyList(), available = false, shortcutUrl = null)

    companion object {
        const val SOURCE_ID = "pricecharting"
    }
}

/**
 * Map a PriceCharting product into a [SourceResult]. Each populated price variant becomes one SOLD
 * listing tagged with the matching condition, giving the engine loose→CIB→sealed price points.
 * Pure and side-effect-free so it can be unit-tested against sample payloads.
 */
fun PriceChartingProductDto.toSourceResult(): SourceResult {
    val title = listOfNotNull(productName, consoleName?.let { "($it)" })
        .joinToString(" ")
        .ifBlank { "Unknown item" }

    val listings = buildList {
        loosePrice?.takeIf { it > 0 }?.let { add(listing(title, it, Condition.ACCEPTABLE)) }
        cibPrice?.takeIf { it > 0 }?.let { add(listing(title, it, Condition.GOOD)) }
        newPrice?.takeIf { it > 0 }?.let { add(listing(title, it, Condition.SEALED)) }
    }

    val ok = status == "success" && listings.isNotEmpty()
    return SourceResult(
        sourceId = PriceChartingSource.SOURCE_ID,
        listings = listings,
        productTitle = productName?.takeIf { it.isNotBlank() },
        category = consoleName,
        available = ok,
        shortcutUrl = MarketplaceUrls.priceCharting(productName ?: (upc ?: "")),
    )
}

private fun listing(title: String, cents: Long, condition: Condition) = MarketListing(
    sourceId = PriceChartingSource.SOURCE_ID,
    title = title,
    price = Money.ofCents(cents),
    type = ListingType.SOLD,
    condition = condition,
)

/** Convert every listing price in this result from USD into the EUR base. */
fun SourceResult.toEur(converter: CurrencyConverter): SourceResult =
    copy(listings = listings.map { it.copy(price = converter.toEur(it.price, Currency.USD)) })
