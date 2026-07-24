package nl.madebypatrick.flipiq.data.source

/**
 * A marketplace we link to but don't pull price data from (no API, and we're not scraping) — e.g.
 * Vinted and Marktplaats. It contributes an "open search" shortcut, never any listings, so no
 * invented prices reach the engine. The repository fills in the shortcut URL from the resolved
 * product title (see [shortcutFor]); [lookup] falls back to the barcode when queried directly.
 */
class ShortcutOnlySource(
    override val id: String,
    override val displayName: String,
    private val urlBuilder: (String) -> String,
) : MarketplaceSource {

    fun shortcutFor(query: String): String = urlBuilder(query)

    override suspend fun lookup(query: ProductQuery): SourceResult = SourceResult(
        sourceId = id,
        listings = emptyList(),
        available = true,
        shortcutUrl = urlBuilder(query.title ?: query.barcode),
    )
}
