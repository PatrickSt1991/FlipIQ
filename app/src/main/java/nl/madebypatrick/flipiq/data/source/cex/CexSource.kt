package nl.madebypatrick.flipiq.data.source.cex

import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.MarketplaceUrls
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.domain.CurrencyConverter
import nl.madebypatrick.flipiq.domain.model.Currency
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money

/**
 * Live CeX source. Keyless, so it's wired in by default; the CeX sell price is a genuine buy
 * opportunity, exposed as an ACTIVE listing (converted from GBP to the EUR base). Network/parse
 * failures degrade to an unavailable, empty result so one flaky source never sinks a scan.
 */
class CexSource(
    private val api: CexApi,
    private val currencyConverter: CurrencyConverter,
) : MarketplaceSource {

    override val id = SOURCE_ID
    override val displayName = "CeX"

    override suspend fun lookup(query: ProductQuery): SourceResult {
        return runCatching {
            api.search(query.barcode)
                .toSourceResult()
                .toEurFromGbp(currencyConverter)
        }.getOrElse {
            SourceResult(sourceId = id, listings = emptyList(), available = false, shortcutUrl = null)
        }
    }

    companion object {
        const val SOURCE_ID = "cex"
        /** Cap the number of boxes we turn into listings to keep results focused. */
        const val MAX_BOXES = 5
    }
}

/**
 * Map a CeX response into a [SourceResult] (prices still in GBP). Pure and side-effect-free so it
 * can be unit-tested against sample payloads.
 */
fun CexResponse.toSourceResult(): SourceResult {
    val body = response
    val boxes = body?.data?.boxes.orEmpty()
        .filter { (it.sellPrice ?: 0.0) > 0.0 }
        .take(CexSource.MAX_BOXES)

    val listings = boxes.map { box ->
        MarketListing(
            sourceId = CexSource.SOURCE_ID,
            title = box.boxName ?: "CeX item",
            price = Money.ofEuros(box.sellPrice ?: 0.0),
            type = ListingType.ACTIVE,
        )
    }

    val title = boxes.firstOrNull()?.boxName
    val ok = body?.ack.equals("Success", ignoreCase = true) && listings.isNotEmpty()
    return SourceResult(
        sourceId = CexSource.SOURCE_ID,
        listings = listings,
        productTitle = title,
        category = boxes.firstOrNull()?.categoryName,
        available = ok,
        shortcutUrl = MarketplaceUrls.cex(title ?: ""),
    )
}

/** Convert every listing price in this result from GBP into the EUR base. */
fun SourceResult.toEurFromGbp(converter: CurrencyConverter): SourceResult =
    copy(listings = listings.map { it.copy(price = converter.toEur(it.price, Currency.GBP)) })
