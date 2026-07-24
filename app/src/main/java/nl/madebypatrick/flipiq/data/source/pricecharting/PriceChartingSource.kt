package nl.madebypatrick.flipiq.data.source.pricecharting

import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.MarketplaceUrls
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money

/**
 * Real PriceCharting integration — the first live marketplace source, sitting behind the same
 * [MarketplaceSource] seam as the mocks. It looks a product up by the scanned UPC and turns
 * PriceCharting's loose/CIB/new price guide into market listings the engine can score.
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
    private val token: String,
) : MarketplaceSource {

    override val id = SOURCE_ID
    override val displayName = "PriceCharting"

    override suspend fun lookup(query: ProductQuery): SourceResult {
        if (token.isBlank()) return unavailable()
        return runCatching { api.productByUpc(token, query.barcode).toSourceResult() }
            .getOrElse { unavailable() }
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
