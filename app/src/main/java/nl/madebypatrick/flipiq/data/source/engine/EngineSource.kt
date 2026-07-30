package nl.madebypatrick.flipiq.data.source.engine

import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money

/**
 * The FlipIQ Engine as a single data source. It calls the engine Worker's `/prices`, which aggregates
 * multiple marketplaces (Marktplaats, eBay, …) server-side, and feeds all returned listings — each
 * tagged with its real marketplace — into the scoring engine. The per-marketplace "open search" chips
 * are separate shortcut sources.
 */
class EngineSource(
    private val api: EngineApi,
    private val engineUrl: String,
    private val appKey: String,
    private val location: suspend () -> String,
) : MarketplaceSource {

    override val id = "engine"
    override val displayName = "eBay & Marktplaats"

    override suspend fun lookup(query: ProductQuery): SourceResult {
        val term = query.title ?: query.barcode
        if (term.isBlank()) return unavailable()
        val endpoint = engineUrl.trimEnd('/') + "/prices"
        return runCatching { api.prices(endpoint, appKey, term, query.barcode.ifBlank { null }, location()) }
            .map { it.toSourceResult() }
            .getOrElse { unavailable() }
    }

    private fun unavailable() =
        SourceResult(sourceId = id, listings = emptyList(), available = false, shortcutUrl = null)
}

/** Map the engine response into listings, each tagged with the marketplace it came from. */
fun EngineResponse.toSourceResult(): SourceResult {
    val mapped = listings.mapNotNull { l ->
        val cents = l.priceCents ?: return@mapNotNull null
        if (cents <= 0) return@mapNotNull null
        MarketListing(
            sourceId = l.source ?: "engine",
            title = l.title ?: "item",
            price = Money.ofCents(cents),
            type = if (l.type == "SOLD") ListingType.SOLD else ListingType.ACTIVE,
            url = l.url,
        )
    }
    return SourceResult(
        sourceId = "engine",
        listings = mapped,
        available = mapped.isNotEmpty(),
        shortcutUrl = null,
        imageUrl = image,
    )
}
