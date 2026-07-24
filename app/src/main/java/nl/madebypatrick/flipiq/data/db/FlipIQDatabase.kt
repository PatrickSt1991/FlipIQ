package nl.madebypatrick.flipiq.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScanEntity::class, InventoryEntity::class, SavedItemEntity::class, PriceAlertEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class FlipIQDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun savedItemDao(): SavedItemDao
    abstract fun priceAlertDao(): PriceAlertDao

    companion object {
        const val NAME = "flipiq.db"
    }
}
