package nl.madebypatrick.flipiq.data.source.engine

import nl.madebypatrick.flipiq.data.source.MarketplaceSource
import nl.madebypatrick.flipiq.data.source.MarketplaceUrls
import nl.madebypatrick.flipiq.data.source.ProductQuery
import nl.madebypatrick.flipiq.data.source.SourceResult
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money

/**
 * Marktplaats as a real data source, via the FlipIQ Engine Worker. Falls back to just a search link
 * if the engine isn't configured or fails. Currently the engine only serves Marktplaats, so this is
 * presented as the Marktplaats source; when the engine grows more providers we can split it out.
 */
class EngineSource(
    private val api: EngineApi,
    private val engineUrl: String,
    private val appKey: String,
) : MarketplaceSource {

    override val id = "marktplaats"
    override val displayName = "Marktplaats"

    val isConfigured: Boolean get() = engineUrl.isNotBlank()

    override suspend fun lookup(query: ProductQuery): SourceResult {
        val term = query.title ?: query.barcode
        val shortcut = MarketplaceUrls.marktplaats(term)
        if (!isConfigured) return SourceResult(id, emptyList(), available = false, shortcutUrl = shortcut)

        val endpoint = engineUrl.trimEnd('/') + "/prices"
        return runCatching { api.prices(endpoint, appKey, term, query.barcode.ifBlank { null }) }
            .map { it.toSourceResult(shortcut) }
            .getOrElse { SourceResult(id, emptyList(), available = false, shortcutUrl = shortcut) }
    }
}

/** Map the engine response into Marktplaats listings (ASKING → active buy opportunities). */
fun EngineResponse.toSourceResult(shortcut: String): SourceResult {
    val listings = listings.mapNotNull { l ->
        val cents = l.priceCents ?: return@mapNotNull null
        if (cents <= 0) return@mapNotNull null
        MarketListing(
            sourceId = "marktplaats",
            title = l.title ?: "Marktplaats item",
            price = Money.ofCents(cents),
            type = if (l.type == "SOLD") ListingType.SOLD else ListingType.ACTIVE,
            url = l.url,
        )
    }
    return SourceResult(
        sourceId = "marktplaats",
        listings = listings,
        available = listings.isNotEmpty(),
        shortcutUrl = shortcut,
    )
}
