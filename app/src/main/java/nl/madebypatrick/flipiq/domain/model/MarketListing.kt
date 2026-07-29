package nl.madebypatrick.flipiq.domain.model

/**
 * A single price data point pulled from one marketplace.
 *
 * [daysAgo] is how many days ago the sale completed (for SOLD listings) — used for sell-speed and
 * trend analysis. It is null for ACTIVE listings, which carry an asking price but no sale date.
 */
data class MarketListing(
    val sourceId: String,
    val title: String,
    val price: Money,
    val type: ListingType,
    val daysAgo: Int? = null,
    val condition: Condition? = null,
    val url: String? = null,
) {
    val isSold: Boolean get() = type == ListingType.SOLD

    /** A guaranteed dealer buy-in (trade-in) offer — deliberately kept out of the resale median. */
    val isTradeIn get() = type == ListingType.TRADE_IN
}
