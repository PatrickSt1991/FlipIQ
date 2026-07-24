package nl.madebypatrick.flipiq.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScanEntity::class, InventoryEntity::class, SavedItemEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class FlipIQDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun savedItemDao(): SavedItemDao

    companion object {
        const val NAME = "flipiq.db"
    }
}
