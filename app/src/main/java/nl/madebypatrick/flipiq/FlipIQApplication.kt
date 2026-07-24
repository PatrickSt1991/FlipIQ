package nl.madebypatrick.flipiq

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import nl.madebypatrick.flipiq.notifications.AlertNotifier
import nl.madebypatrick.flipiq.work.PriceAlertWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FlipIQApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var alertNotifier: AlertNotifier

    // On-demand WorkManager initialization wired to Hilt's worker factory.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        alertNotifier.ensureChannel()
        schedulePriceAlerts()
    }

    private fun schedulePriceAlerts() {
        val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PriceAlertWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
