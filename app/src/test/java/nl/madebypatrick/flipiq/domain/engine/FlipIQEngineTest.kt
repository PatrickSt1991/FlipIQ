package nl.madebypatrick.flipiq.domain.engine

import com.google.common.truth.Truth.assertThat
import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.DealTier
import nl.madebypatrick.flipiq.domain.model.EngineInput
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.MarketTrend
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ProfitSettings
import nl.madebypatrick.flipiq.domain.model.SellSpeed
import org.junit.Test

class FlipIQEngineTest {

    private val engine = FlipIQEngine()

    private fun sold(euros: Double, daysAgo: Int? = null) = MarketListing(
        sourceId = "test",
        title = "item",
        price = Money.ofEuros(euros),
        type = ListingType.SOLD,
        daysAgo = daysAgo,
    )

    private fun active(euros: Double) = MarketListing(
        sourceId = "test",
        title = "item",
        price = Money.ofEuros(euros),
        type = ListingType.ACTIVE,
    )

    // --- Statistics -------------------------------------------------------------------------

    @Test
    fun `median of odd count is the middle value`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(10.0), sold(12.0), sold(20.0))))
        assertThat(rec.stats.median).isEqualTo(Money.ofEuros(12.0))
    }

    @Test
    fun `median of even count is the mean of the two middle values`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(10.0), sold(12.0), sold(14.0), sold(20.0))))
        assertThat(rec.stats.median).isEqualTo(Money.ofEuros(13.0))
    }

    @Test
    fun `average, lowest and highest are computed over sold listings`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(10.0), sold(20.0), sold(30.0))))
        assertThat(rec.stats.average).isEqualTo(Money.ofEuros(20.0))
        assertThat(rec.stats.lowest).isEqualTo(Money.ofEuros(10.0))
        assertThat(rec.stats.highest).isEqualTo(Money.ofEuros(30.0))
        assertThat(rec.stats.soldCount).isEqualTo(3)
    }

    @Test
    fun `active listings do not count toward sold statistics`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(10.0), sold(20.0), active(3.0))))
        assertThat(rec.stats.soldCount).isEqualTo(2)
        assertThat(rec.stats.average).isEqualTo(Money.ofEuros(15.0))
    }

    // --- Pricing ----------------------------------------------------------------------------

    @Test
    fun `recommended buy price honours both the profit and ROI targets`() {
        val settings = ProfitSettings(
            minProfit = Money.ofEuros(5.0),
            minRoi = 0.30,
            includeFees = false,
            includeShipping = false,
        )
        // Median resale 50.00, no fees/shipping ⇒ net = 50.00.
        val rec = engine.evaluate(
            EngineInput(List(6) { sold(50.0, daysAgo = it * 5) }, settings = settings),
        )
        // profit constraint ⇒ buy ≤ 45.00; ROI constraint ⇒ buy ≤ 50 / 1.30 = 38.46. Tighter wins.
        assertThat(rec.recommendedBuyPrice).isEqualTo(Money.ofEuros(38.46))
        // Buying there clears at least the minimum profit and ROI.
        assertThat(rec.expectedProfit.cents).isAtLeast(Money.ofEuros(5.0).cents)
        assertThat(rec.roi).isAtLeast(0.30)
    }

    @Test
    fun `fees and shipping reduce net resale`() {
        val settings = ProfitSettings(includeFees = true, includeShipping = true, marketplaceFee = 0.10, shippingCost = Money.ofEuros(4.0))
        val rec = engine.evaluate(EngineInput(List(6) { sold(20.0) }, settings = settings))
        // 20.00 − 10% fee (2.00) − 4.00 shipping = 14.00 net.
        assertThat(rec.netResale).isEqualTo(Money.ofEuros(14.0))
    }

    @Test
    fun `condition and completeness scale the resale estimate`() {
        val base = engine.evaluate(EngineInput(List(6) { sold(100.0) }, condition = Condition.GOOD, completeness = Completeness.COMPLETE))
        val loosePoor = engine.evaluate(EngineInput(List(6) { sold(100.0) }, condition = Condition.POOR, completeness = Completeness.LOOSE))
        assertThat(loosePoor.estimatedResale).isLessThan(base.estimatedResale)
        // POOR (0.65) * LOOSE (0.75) = 0.4875 ⇒ 48.75 from a 100.00 median.
        assertThat(loosePoor.estimatedResale).isEqualTo(Money.ofEuros(48.75))
    }

    @Test
    fun `buy tiers form an ascending ladder capped at net resale`() {
        val rec = engine.evaluate(EngineInput(List(6) { sold(100.0) }, settings = ProfitSettings(includeFees = false, includeShipping = false)))
        val prices = rec.buyTiers.map { it.maxPrice.cents }
        assertThat(prices).isInOrder()
        assertThat(rec.buyTiers.last().maxPrice).isEqualTo(rec.netResale)
    }

    // --- Deal Score -------------------------------------------------------------------------

    @Test
    fun `a wide margin fast-selling item scores higher than a thin slow one`() {
        val greatFlip = engine.evaluate(
            EngineInput(
                listings = List(15) { sold(40.0, daysAgo = it + 1) } + active(8.0),
                settings = ProfitSettings(includeFees = false, includeShipping = false),
            ),
        )
        val poorFlip = engine.evaluate(
            EngineInput(
                listings = List(3) { sold(12.0, daysAgo = it * 40) } + active(11.0),
                settings = ProfitSettings(includeFees = false, includeShipping = false),
            ),
        )
        assertThat(greatFlip.dealScore.value).isGreaterThan(poorFlip.dealScore.value)
        assertThat(greatFlip.dealScore.tier).isAnyOf(DealTier.GREAT_DEAL, DealTier.BUY_IMMEDIATELY)
    }

    @Test
    fun `deal score is zero when there is no data`() {
        val rec = engine.evaluate(EngineInput(emptyList()))
        assertThat(rec.dealScore.value).isEqualTo(0)
        assertThat(rec.dealScore.tier).isEqualTo(DealTier.SKIP)
    }

    @Test
    fun `a lower asking price yields a better deal score than a higher one`() {
        val listings = List(10) { sold(30.0, daysAgo = it + 1) }
        val cheap = engine.evaluate(EngineInput(listings, askingPrice = Money.ofEuros(8.0)))
        val pricey = engine.evaluate(EngineInput(listings, askingPrice = Money.ofEuros(25.0)))
        assertThat(cheap.dealScore.value).isGreaterThan(pricey.dealScore.value)
    }

    // --- Sell speed & trend -----------------------------------------------------------------

    @Test
    fun `many sales in a short window read as very fast`() {
        val rec = engine.evaluate(EngineInput(List(20) { sold(15.0, daysAgo = (it % 10) + 1) }))
        assertThat(rec.sellSpeed).isEqualTo(SellSpeed.VERY_FAST)
    }

    @Test
    fun `few sales spread over months read as slow`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(15.0, daysAgo = 20), sold(15.0, daysAgo = 200))))
        assertThat(rec.sellSpeed).isEqualTo(SellSpeed.SLOW)
    }

    @Test
    fun `rising prices are detected as a rising trend`() {
        // Older sales cheap, newer sales expensive (daysAgo larger = older).
        val listings = listOf(
            sold(10.0, daysAgo = 90), sold(10.0, daysAgo = 80),
            sold(20.0, daysAgo = 10), sold(20.0, daysAgo = 5),
        )
        assertThat(engine.evaluate(EngineInput(listings)).trend).isEqualTo(MarketTrend.RISING)
    }

    @Test
    fun `flat prices are detected as a stable trend`() {
        val listings = List(8) { sold(15.0, daysAgo = it * 10) }
        assertThat(engine.evaluate(EngineInput(listings)).trend).isEqualTo(MarketTrend.STABLE)
    }

    // --- Confidence -------------------------------------------------------------------------

    @Test
    fun `tightly clustered prices give higher confidence than volatile ones`() {
        val tight = engine.evaluate(EngineInput(List(12) { sold(20.0) }))
        val volatile = engine.evaluate(EngineInput(listOf(sold(5.0), sold(40.0), sold(8.0), sold(35.0), sold(12.0), sold(50.0), sold(6.0), sold(45.0), sold(9.0), sold(38.0), sold(7.0), sold(42.0))))
        assertThat(tight.confidence).isGreaterThan(volatile.confidence)
        assertThat(tight.confidence).isAtLeast(90)
    }

    @Test
    fun `confidence grows with the number of sales`() {
        val few = engine.evaluate(EngineInput(List(2) { sold(20.0) }))
        val many = engine.evaluate(EngineInput(List(12) { sold(20.0) }))
        assertThat(many.confidence).isGreaterThan(few.confidence)
    }

    // --- Profit-Mode viability --------------------------------------------------------------

    @Test
    fun `too few sales makes the item non-viable`() {
        val rec = engine.evaluate(EngineInput(List(2) { sold(30.0) }, settings = ProfitSettings(minSales = 5)))
        assertThat(rec.viable).isFalse()
        assertThat(rec.notes.any { it.contains("recent sales") }).isTrue()
    }

    @Test
    fun `resale below the floor makes the item non-viable`() {
        val rec = engine.evaluate(EngineInput(List(6) { sold(6.0) }, settings = ProfitSettings(ignoreBelow = Money.ofEuros(10.0), minSales = 1)))
        assertThat(rec.viable).isFalse()
        assertThat(rec.notes.any { it.contains("floor") }).isTrue()
    }

    @Test
    fun `damaged item is rejected when profit mode ignores damaged`() {
        val rec = engine.evaluate(
            EngineInput(
                List(6) { sold(30.0) },
                condition = Condition.POOR,
                settings = ProfitSettings(ignoreDamaged = true, minSales = 1, ignoreBelow = Money.ZERO),
            ),
        )
        assertThat(rec.viable).isFalse()
        assertThat(rec.notes.any { it.contains("poor condition") }).isTrue()
    }

    @Test
    fun `a healthy item that meets every target is viable`() {
        val rec = engine.evaluate(
            EngineInput(
                List(10) { sold(30.0, daysAgo = it + 1) } + active(10.0),
                settings = ProfitSettings(minSales = 5, ignoreBelow = Money.ofEuros(10.0)),
            ),
        )
        assertThat(rec.viable).isTrue()
        assertThat(rec.notes).contains("Meets your Profit Mode targets.")
    }
}
