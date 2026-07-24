package nl.madebypatrick.flipiq.domain.model

/** Physical condition of the item being scanned. */
enum class Condition { SEALED, MINT, GOOD, ACCEPTABLE, POOR }

/** How complete the item is — a loose cartridge is worth far less than a complete-in-box copy. */
enum class Completeness { SEALED, COMPLETE, LOOSE }

/** Whether a data point is a completed sale or a currently-active listing. */
enum class ListingType { SOLD, ACTIVE }

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
