package nl.madebypatrick.flipiq.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.madebypatrick.flipiq.data.db.InventoryDao
import nl.madebypatrick.flipiq.data.db.InventoryEntity
import nl.madebypatrick.flipiq.data.db.ScanDao
import nl.madebypatrick.flipiq.data.db.ScanEntity
import nl.madebypatrick.flipiq.domain.model.DealTier
import nl.madebypatrick.flipiq.domain.model.InventoryItem
import nl.madebypatrick.flipiq.domain.model.InventoryStatus
import nl.madebypatrick.flipiq.domain.model.InventorySummary
import nl.madebypatrick.flipiq.domain.model.Money
import nl.madebypatrick.flipiq.domain.model.ScanAnalysis
import nl.madebypatrick.flipiq.domain.model.ScanRecord
import nl.madebypatrick.flipiq.domain.model.SellSpeed

/**
 * Persistence for the Collection features: scan history, inventory and the profit tracker.
 * Maps between Room entities and domain models so the rest of the app never sees the database.
 *
 * [now] is injectable so time-dependent behaviour stays testable.
 */
class CollectionRepository(
    private val scanDao: ScanDao,
    private val inventoryDao: InventoryDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    val scanHistory: Flow<List<ScanRecord>> =
        scanDao.recent().map { list -> list.map { it.toDomain() } }

    val inventory: Flow<List<InventoryItem>> =
        inventoryDao.all().map { list -> list.map { it.toDomain() } }

    val inventorySummary: Flow<InventorySummary> =
        inventory.map { InventorySummary.from(it) }

    /** Record a scan/analysis into history. Returns the new row id. */
    suspend fun recordScan(analysis: ScanAnalysis): Long {
        val r = analysis.recommendation
        return scanDao.insert(
            ScanEntity(
                barcode = analysis.product.barcode,
                title = analysis.product.title,
                category = analysis.product.category,
                dealScore = r.dealScore.value,
                tier = r.dealScore.tier.name,
                estimatedResaleCents = r.estimatedResale.cents,
                recommendedBuyCents = r.recommendedBuyPrice.cents,
                sellSpeed = r.sellSpeed.name,
                scannedAt = now(),
            ),
        )
    }

    suspend fun addToInventory(
        barcode: String,
        title: String,
        buyPrice: Money,
        estimatedResale: Money,
    ): Long = inventoryDao.insert(
        InventoryEntity(
            barcode = barcode,
            title = title,
            buyPriceCents = buyPrice.cents,
            estimatedResaleCents = estimatedResale.cents,
            boughtAt = now(),
            status = InventoryStatus.IN_STOCK.name,
            soldPriceCents = null,
            soldAt = null,
        ),
    )

    suspend fun markSold(id: Long, soldPrice: Money) {
        val existing = inventoryDao.byId(id) ?: return
        inventoryDao.update(
            existing.copy(
                status = InventoryStatus.SOLD.name,
                soldPriceCents = soldPrice.cents,
                soldAt = now(),
            ),
        )
    }
}

private fun ScanEntity.toDomain() = ScanRecord(
    id = id,
    barcode = barcode,
    title = title,
    category = category,
    dealScore = dealScore,
    tier = runCatching { DealTier.valueOf(tier) }.getOrDefault(DealTier.SKIP),
    estimatedResale = Money(estimatedResaleCents),
    recommendedBuy = Money(recommendedBuyCents),
    sellSpeed = runCatching { SellSpeed.valueOf(sellSpeed) }.getOrDefault(SellSpeed.MEDIUM),
    scannedAt = scannedAt,
)

private fun InventoryEntity.toDomain() = InventoryItem(
    id = id,
    barcode = barcode,
    title = title,
    buyPrice = Money(buyPriceCents),
    estimatedResale = Money(estimatedResaleCents),
    boughtAt = boughtAt,
    status = runCatching { InventoryStatus.valueOf(status) }.getOrDefault(InventoryStatus.IN_STOCK),
    soldPrice = soldPriceCents?.let { Money(it) },
    soldAt = soldAt,
)
