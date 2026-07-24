package nl.madebypatrick.flipiq.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import nl.madebypatrick.flipiq.data.repository.AlertRepository
import nl.madebypatrick.flipiq.data.repository.PriceRepository
import nl.madebypatrick.flipiq.domain.PriceAlertEvaluator
import nl.madebypatrick.flipiq.notifications.AlertNotifier

/**
 * Periodically re-checks every active price alert: re-analyses the item and, if it's now buyable at
 * or below the target (and outside the cooldown), posts a notification. A single failing alert never
 * fails the whole run.
 */
@HiltWorker
class PriceAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val alertRepository: AlertRepository,
    private val priceRepository: PriceRepository,
    private val notifier: AlertNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val alerts = runCatching { alertRepository.activeAlertsOnce() }.getOrElse { return Result.retry() }
        val now = System.currentTimeMillis()

        for (alert in alerts) {
            runCatching {
                val analysis = priceRepository.analyze(alert.barcode)
                if (PriceAlertEvaluator.shouldNotify(alert, analysis.recommendation, now)) {
                    val best = analysis.recommendation.bestBuyPrice ?: return@runCatching
                    notifier.notifyPriceHit(alert.id, alert.title, alert.targetPrice, best)
                    alertRepository.markNotified(alert)
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "price-alerts"
    }
}
