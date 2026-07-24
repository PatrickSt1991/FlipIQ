package nl.madebypatrick.flipiq.domain

import com.google.common.truth.Truth.assertThat
import nl.madebypatrick.flipiq.domain.engine.FlipIQEngine
import nl.madebypatrick.flipiq.domain.model.EngineInput
import nl.madebypatrick.flipiq.domain.model.ListingType
import nl.madebypatrick.flipiq.domain.model.MarketListing
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.PriceAlert
import org.junit.Test

class PriceAlertEvaluatorTest {

    private val engine = FlipIQEngine()

    private fun sold(euros: Double) = MarketListing("t", "i", Money.ofEuros(euros), ListingType.SOLD)
    private fun active(euros: Double) = MarketListing("t", "i", Money.ofEuros(euros), ListingType.ACTIVE)

    private fun alert(target: Double, lastNotifiedAt: Long? = null, active: Boolean = true) = PriceAlert(
        id = 1, barcode = "b", title = "i", targetPrice = Money.ofEuros(target),
        active = active, createdAt = 0L, lastNotifiedAt = lastNotifiedAt,
    )

    @Test
    fun `engine exposes the cheapest active listing as bestBuyPrice`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(20.0), active(9.0), active(7.5))))
        assertThat(rec.bestBuyPrice).isEqualTo(Money.ofEuros(7.5))
    }

    @Test
    fun `bestBuyPrice is null when nothing is currently for sale`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(20.0), sold(22.0))))
        assertThat(rec.bestBuyPrice).isNull()
    }

    @Test
    fun `notifies when the best price is at or below target`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(20.0), active(8.0))))
        assertThat(PriceAlertEvaluator.shouldNotify(alert(target = 10.0), rec, now = 0L)).isTrue()
    }

    @Test
    fun `does not notify when the price is still above target`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(20.0), active(12.0))))
        assertThat(PriceAlertEvaluator.shouldNotify(alert(target = 10.0), rec, now = 0L)).isFalse()
    }

    @Test
    fun `inactive alerts never notify`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(20.0), active(5.0))))
        assertThat(PriceAlertEvaluator.shouldNotify(alert(target = 10.0, active = false), rec, now = 0L)).isFalse()
    }

    @Test
    fun `respects the re-notify cooldown window`() {
        val rec = engine.evaluate(EngineInput(listOf(sold(20.0), active(5.0))))
        val recentlyNotified = alert(target = 10.0, lastNotifiedAt = 1_000L)
        // 1 hour later — still inside the 24h window.
        assertThat(PriceAlertEvaluator.shouldNotify(recentlyNotified, rec, now = 1_000L + 3_600_000)).isFalse()
        // 25 hours later — window elapsed.
        assertThat(PriceAlertEvaluator.shouldNotify(recentlyNotified, rec, now = 1_000L + 25 * 3_600_000L)).isTrue()
    }
}
