package nl.madebypatrick.flipiq.domain.model

/** Physical condition of the item being scanned. */
enum class Condition { SEALED, MINT, GOOD, ACCEPTABLE, POOR }

/** How complete the item is — a loose cartridge is worth far less than a complete-in-box copy. */
enum class Completeness { SEALED, COMPLETE, LOOSE }

/**
 * Whether a data point is a completed sale, a currently-active listing, or a guaranteed trade-in
 * (buy-in) offer.
 *
 * [TRADE_IN] is a dealer's guaranteed buy-in price (what they pay *you*), not a market value. The
 * engine filters explicitly for [SOLD] and [ACTIVE], so trade-in points fall through both paths and
 * never enter the resale median — a wholesale bid must not drag a Deal Score down. See Reway (§3).
 */
enum class ListingType { SOLD, ACTIVE, TRADE_IN }

/** Direction the recent sold-price is trending. */
enum class MarketTrend { RISING, STABLE, FALLING }

/**
 * How quickly the item typically sells. Buckets mirror the README:
 * days / 1–2 weeks / 2–8 weeks / months.
 */
enum class SellSpeed { VERY_FAST, FAST, MEDIUM, SLOW }

/** Deal Score bucket (0–100 → recommendation). */
enum class DealTier { BUY_IMMEDIATELY, GREAT_DEAL, FAIR_PRICE, LOW_PROFIT, SKIP }

/** Quality of a per-price buy tier used to build the "buy below €X" ladder. */
enum class BuyTierLevel { EXCELLENT, GOOD, FAIR, SKIP }
