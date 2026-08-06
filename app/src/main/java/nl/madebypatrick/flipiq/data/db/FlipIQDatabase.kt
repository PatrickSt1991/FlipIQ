package nl.madebypatrick.flipiq.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScanEntity::class, InventoryEntity::class, SavedItemEntity::class, PriceAlertEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class FlipIQDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun savedItemDao(): SavedItemDao
    abstract fun priceAlertDao(): PriceAlertDao

    companion object {
        const val NAME = "flipiq.db"

        /** v4 adds inventory.imageUrl (cover art) — a nullable column, so it preserves existing rows. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory ADD COLUMN imageUrl TEXT")
            }
        }
    }
}
