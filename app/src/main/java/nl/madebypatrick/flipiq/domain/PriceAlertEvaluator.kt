package nl.madebypatrick.flipiq.domain

import nl.madebypatrick.flipiq.domain.model.FlipRecommendation
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.PriceAlert

/** Pure decision logic for whether a price alert should fire. Kept separate so it's unit-testable. */
object PriceAlertEvaluator {

    /** Minimum gap between notifications for the same alert (24h), so it doesn't nag daily. */
    const val RENOTIFY_WINDOW_MS = 24L * 60 * 60 * 1000

    /**
     * True when the item is currently buyable at or below the target price and the alert isn't in
     * its cooldown window.
     */
    fun shouldNotify(alert: PriceAlert, rec: FlipRecommendation, now: Long): Boolean {
        if (!alert.active) return false
        val best = rec.bestBuyPrice ?: return false
        if (best > alert.targetPrice) return false
        val last = alert.lastNotifiedAt ?: return true
        return now - last >= RENOTIFY_WINDOW_MS
    }

    /** Convenience for callers that only have the current best buy price. */
    fun isPriceMet(target: Money, bestBuyPrice: Money?): Boolean =
        bestBuyPrice != null && bestBuyPrice <= target
}
