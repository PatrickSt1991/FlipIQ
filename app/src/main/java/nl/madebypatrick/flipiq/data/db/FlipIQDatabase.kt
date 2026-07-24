package nl.madebypatrick.flipiq.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScanEntity::class, InventoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FlipIQDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        const val NAME = "flipiq.db"
    }
}
