package nl.madebypatrick.flipiq.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities. Money is stored as integer cents and enums as their names, keeping the schema
 * primitive and portable; mapping to/from domain models happens in the repository.
 */

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val title: String,
    val category: String?,
    val dealScore: Int,
    val tier: String,
    val estimatedResaleCents: Long,
    val recommendedBuyCents: Long,
    val sellSpeed: String,
    val scannedAt: Long,
)

@Entity(tableName = "saved_items")
data class SavedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val title: String,
    val list: String,
    val savedAt: Long,
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val title: String,
    val targetPriceCents: Long,
    val active: Boolean,
    val createdAt: Long,
    val lastNotifiedAt: Long?,
)

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val title: String,
    val buyPriceCents: Long,
    val estimatedResaleCents: Long,
    val boughtAt: Long,
    val status: String,
    val soldPriceCents: Long?,
    val soldAt: Long?,
    val imageUrl: String? = null,
    val category: String? = null,
)
