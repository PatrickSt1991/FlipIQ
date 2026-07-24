package nl.madebypatrick.flipiq.domain.engine

import nl.madebypatrick.flipiq.domain.model.BuyTier
import nl.madebypatrick.flipiq.domain.model.BuyTierLevel
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.DealScore
import nl.madebypatrick.flipiq.domain.model.DealTier
import nl.madebypatrick.flipiq.domain.model.EngineInput
import nl.madebypatrick.flipiq.domain.model.FlipRecommendation
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.MarketTrend
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.PriceStatistics
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.domain.model.SellSpeed
import kotlin.math.sqrt

/**
 * The FlipIQ Engine.
 *
 * Pure, deterministic Kotlin — no Android, no I/O, no clock — so it is fully unit-testable. Given a
 * set of marketplace listings, the item's condition, and the user's Profit Mode settings, it answers
 * the only question that matters: *should I buy this, and for how much?*
 *
 * Pipeline: sold-price statistics → condition-adjusted resale → net after fees/shipping →
 * recommended max buy (that still clears the profit + ROI targets) → sell speed, trend, confidence →
 * a 0–100 Deal Score → the "buy below €X" ladder → Profit-Mode viability check.
 */
class FlipIQEngine(private val config: EngineConfig = EngineConfig.DEFAULT) {

    fun evaluate(input: EngineInput): FlipRecommendation {
        val settings = input.settings
        val soldPrices = input.listings
            .filter { it.isSold }
            .map { it.price.cents }
            .sorted()

        val stats = computeStatistics(soldPrices)

        // Resale estimate: median sold, adjusted for this item's condition & completeness.
        val conditionAdj = config.conditionMultiplier(input.condition) *
            config.completenessMultiplier(input.completeness)
        val estimatedResale = (stats.median * conditionAdj).coerceAtLeastZero()

        val netResale = netAfterCosts(estimatedResale, settings)
        val recommendedBuy = recommendedBuyPrice(netResale, settings)
        val expectedProfit = (netResale - recommendedBuy).coerceAtLeastZero()
        val roi = if (recommendedBuy.cents > 0) expectedProfit.cents.toDouble() / recommendedBuy.cents else 0.0

        val sellSpeed = computeSellSpeed(input.listings)
        val trend = computeTrend(input.listings)
        val confidence = computeConfidence(stats)

        // Acquisition reference for the Deal Score: what you'd actually pay to get the item. The
        // asking price if the user entered one; otherwise the cheapest currently-buyable (active)
        // listing; and only as a last resort the lowest recent sale.
        val lowestActive = input.listings
            .filter { it.type == ListingType.ACTIVE }
            .minByOrNull { it.price.cents }
            ?.price
        val acquisitionReference = input.askingPrice ?: lowestActive ?: stats.lowest
        val dealScore = computeDealScore(netResale, acquisitionReference, stats, sellSpeed, trend)

        val buyTiers = buildBuyTiers(netResale)
        val (viable, notes) = evaluateViability(input, stats, estimatedResale)

        return FlipRecommendation(
            stats = stats,
            estimatedResale = estimatedResale,
            netResale = netResale,
            recommendedBuyPrice = recommendedBuy,
            expectedProfit = expectedProfit,
            roi = roi,
            dealScore = dealScore,
            sellSpeed = sellSpeed,
            confidence = confidence,
            trend = trend,
            buyTiers = buyTiers,
            viable = viable,
            notes = notes,
        )
    }

    // --- Statistics -------------------------------------------------------------------------

    private fun computeStatistics(sortedCents: List<Long>): PriceStatistics {
        if (sortedCents.isEmpty()) return PriceStatistics.EMPTY

        val count = sortedCents.size
        val sum = sortedCents.sum()
        val mean = sum.toDouble() / count
        val median = if (count % 2 == 1) {
            sortedCents[count / 2].toDouble()
        } else {
            (sortedCents[count / 2 - 1] + sortedCents[count / 2]) / 2.0
        }
        val variance = if (count > 1) {
            sortedCents.sumOf { val d = it - mean; d * d } / count
        } else 0.0
        val dispersion = if (mean > 0) sqrt(variance) / mean else 0.0

        return PriceStatistics(
            soldCount = count,
            average = Money(Math.round(mean)),
            median = Money(Math.round(median)),
            lowest = Money(sortedCents.first()),
            highest = Money(sortedCents.last()),
            dispersion = dispersion,
        )
    }

    // --- Pricing ----------------------------------------------------------------------------

    private fun netAfterCosts(resale: Money, settings: ProfitSettings): Money {
        var net = resale
        if (settings.includeFees) net -= resale * settings.marketplaceFee
        if (settings.includeShipping) net -= settings.shippingCost
        return net.coerceAtLeastZero()
    }

    /**
     * Highest buy price that still satisfies *both* the minimum-profit and minimum-ROI targets:
     *   profit target → buy ≤ net − minProfit
     *   ROI target    → net/buy ≥ 1 + minRoi  ⇒  buy ≤ net / (1 + minRoi)
     */
    private fun recommendedBuyPrice(netResale: Money, settings: ProfitSettings): Money {
        val byProfit = netResale - settings.minProfit
        val byRoi = netResale / (1.0 + settings.minRoi)
        return minOf(byProfit, byRoi).coerceAtLeastZero()
    }

    private fun buildBuyTiers(netResale: Money): List<BuyTier> = listOf(
        BuyTier(BuyTierLevel.EXCELLENT, netResale * config.excellentFraction),
        BuyTier(BuyTierLevel.GOOD, netResale * config.goodFraction),
        BuyTier(BuyTierLevel.FAIR, netResale * config.fairFraction),
        BuyTier(BuyTierLevel.SKIP, netResale),
    )

    // --- Sell speed & trend -----------------------------------------------------------------

    private fun computeSellSpeed(listings: List<MarketListing>): SellSpeed {
        val dated = listings.filter { it.isSold && it.daysAgo != null }
        val soldCount = listings.count { it.isSold }
        if (soldCount == 0) return SellSpeed.SLOW

        // Average days between sales over the observed window.
        val windowDays = dated.mapNotNull { it.daysAgo }.maxOrNull()?.coerceAtLeast(1)
            ?: config.defaultWindowDays
        val avgDaysBetweenSales = windowDays.toDouble() / soldCount

        return when {
            avgDaysBetweenSales <= config.veryFastMaxDays -> SellSpeed.VERY_FAST
            avgDaysBetweenSales <= config.fastMaxDays -> SellSpeed.FAST
            avgDaysBetweenSales <= config.mediumMaxDays -> SellSpeed.MEDIUM
            else -> SellSpeed.SLOW
        }
    }

    private fun computeTrend(listings: List<MarketListing>): MarketTrend {
        val dated = listings.filter { it.isSold && it.daysAgo != null }
            .sortedByDescending { it.daysAgo }
        if (dated.size < 4) return MarketTrend.STABLE

        // Split chronologically into older and newer halves, compare mean price.
        val mid = dated.size / 2
        val older = dated.subList(0, mid)
        val newer = dated.subList(mid, dated.size)
        val olderMean = older.map { it.price.cents }.average()
        val newerMean = newer.map { it.price.cents }.average()
        if (olderMean <= 0) return MarketTrend.STABLE

        val change = (newerMean - olderMean) / olderMean
        return when {
            change > config.trendThreshold -> MarketTrend.RISING
            change < -config.trendThreshold -> MarketTrend.FALLING
            else -> MarketTrend.STABLE
        }
    }

    // --- Confidence -------------------------------------------------------------------------

    private fun computeConfidence(stats: PriceStatistics): Int {
        if (!stats.hasData) return 0
        val countFactor = (stats.soldCount.toDouble() / config.salesForFullConfidence).coerceIn(0.0, 1.0)
        // Tight price clustering → trustworthy estimate; wild spread → discount it.
        val dispersionFactor = (1.0 - stats.dispersion).coerceIn(0.2, 1.0)
        return (100 * countFactor * dispersionFactor).toInt().coerceIn(0, 100)
    }

    // --- Deal Score -------------------------------------------------------------------------

    private fun computeDealScore(
        netResale: Money,
        acquisitionReference: Money,
        stats: PriceStatistics,
        sellSpeed: SellSpeed,
        trend: MarketTrend,
    ): DealScore {
        if (!stats.hasData || netResale.cents <= 0 || acquisitionReference.cents <= 0) {
            return DealScore(0, DealTier.SKIP)
        }

        val profit = (netResale - acquisitionReference).coerceAtLeastZero()
        val roiAtReference = profit.cents.toDouble() / acquisitionReference.cents
        val profitability = (roiAtReference / config.roiForFullProfitability).coerceIn(0.0, 1.0)

        val liquidity = (stats.soldCount.toDouble() / config.salesForFullLiquidity).coerceIn(0.0, 1.0)

        val velocity = when (sellSpeed) {
            SellSpeed.VERY_FAST -> 1.0
            SellSpeed.FAST -> 0.8
            SellSpeed.MEDIUM -> 0.5
            SellSpeed.SLOW -> 0.2
        }

        val trendFactor = when (trend) {
            MarketTrend.RISING -> 1.0
            MarketTrend.STABLE -> 0.6
            MarketTrend.FALLING -> 0.2
        }

        val score = 100 * (
            config.weightProfitability * profitability +
                config.weightLiquidity * liquidity +
                config.weightVelocity * velocity +
                config.weightTrend * trendFactor
            )
        val value = score.toInt().coerceIn(0, 100)
        return DealScore(value, tierFor(value))
    }

    private fun tierFor(score: Int): DealTier = when {
        score >= config.tierBuyImmediately -> DealTier.BUY_IMMEDIATELY
        score >= config.tierGreatDeal -> DealTier.GREAT_DEAL
        score >= config.tierFairPrice -> DealTier.FAIR_PRICE
        score >= config.tierLowProfit -> DealTier.LOW_PROFIT
        else -> DealTier.SKIP
    }

    // --- Profit-Mode viability --------------------------------------------------------------

    private fun evaluateViability(
        input: EngineInput,
        stats: PriceStatistics,
        estimatedResale: Money,
    ): Pair<Boolean, List<String>> {
        val settings = input.settings
        val notes = mutableListOf<String>()
        var viable = true

        if (!stats.hasData) {
            notes += "No sold-price data found for this item."
            return false to notes
        }
        if (stats.soldCount < settings.minSales) {
            viable = false
            notes += "Only ${stats.soldCount} recent sales (need ${settings.minSales})."
        }
        if (estimatedResale < settings.ignoreBelow) {
            viable = false
            notes += "Estimated resale ${estimatedResale} is below your ${settings.ignoreBelow} floor."
        }
        if (settings.ignoreDamaged && input.condition == Condition.POOR) {
            viable = false
            notes += "Item is in poor condition and Profit Mode ignores damaged items."
        }
        if (settings.ignoreIncomplete &&
            input.completeness == nl.madebypatrick.flipiq.domain.model.Completeness.LOOSE
        ) {
            viable = false
            notes += "Item is incomplete/loose and Profit Mode ignores incomplete items."
        }
        if (viable) notes += "Meets your Profit Mode targets."
        return viable to notes
    }
}
