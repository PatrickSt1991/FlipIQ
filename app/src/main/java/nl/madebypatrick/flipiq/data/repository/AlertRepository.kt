package nl.madebypatrick.flipiq.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.madebypatrick.flipiq.data.db.PriceAlertDao
import nl.madebypatrick.flipiq.data.db.PriceAlertEntity
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.PriceAlert

/** Persistence for price alerts (the watch-list the background worker checks). */
class AlertRepository(
    private val dao: PriceAlertDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val alerts: Flow<List<PriceAlert>> = dao.all().map { rows -> rows.map { it.toDomain() } }

    fun hasActiveAlert(barcode: String): Flow<Boolean> = dao.hasActiveAlert(barcode)

    suspend fun create(barcode: String, title: String, target: Money): Long =
        dao.insert(
            PriceAlertEntity(
                barcode = barcode,
                title = title,
                targetPriceCents = target.cents,
                active = true,
                createdAt = now(),
                lastNotifiedAt = null,
            ),
        )

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun activeAlertsOnce(): List<PriceAlert> = dao.activeAlerts().map { it.toDomain() }

    suspend fun markNotified(alert: PriceAlert) {
        dao.update(alert.toEntity().copy(lastNotifiedAt = now()))
    }
}

private fun PriceAlertEntity.toDomain() = PriceAlert(
    id = id,
    barcode = barcode,
    title = title,
    targetPrice = Money(targetPriceCents),
    active = active,
    createdAt = createdAt,
    lastNotifiedAt = lastNotifiedAt,
)

private fun PriceAlert.toEntity() = PriceAlertEntity(
    id = id,
    barcode = barcode,
    title = title,
    targetPriceCents = targetPrice.cents,
    active = active,
    createdAt = createdAt,
    lastNotifiedAt = lastNotifiedAt,
)
