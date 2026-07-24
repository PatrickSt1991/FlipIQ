package nl.madebypatrick.flipiq.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Query("SELECT * FROM scans ORDER BY scannedAt DESC LIMIT :limit")
    fun recent(limit: Int = 200): Flow<List<ScanEntity>>

    @Query("DELETE FROM scans")
    suspend fun clear()
}

@Dao
interface SavedItemDao {
    @Insert
    suspend fun insert(item: SavedItemEntity): Long

    @Query("SELECT * FROM saved_items WHERE list = :list ORDER BY savedAt DESC")
    fun byList(list: String): Flow<List<SavedItemEntity>>

    @Query("SELECT COUNT(*) > 0 FROM saved_items WHERE barcode = :barcode AND list = :list")
    fun isSaved(barcode: String, list: String): Flow<Boolean>

    @Query("DELETE FROM saved_items WHERE barcode = :barcode AND list = :list")
    suspend fun remove(barcode: String, list: String)
}

@Dao
interface PriceAlertDao {
    @Insert
    suspend fun insert(alert: PriceAlertEntity): Long

    @Update
    suspend fun update(alert: PriceAlertEntity)

    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun all(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE active = 1")
    suspend fun activeAlerts(): List<PriceAlertEntity>

    @Query("SELECT COUNT(*) > 0 FROM price_alerts WHERE barcode = :barcode AND active = 1")
    fun hasActiveAlert(barcode: String): Flow<Boolean>

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface InventoryDao {
    @Insert
    suspend fun insert(item: InventoryEntity): Long

    @Update
    suspend fun update(item: InventoryEntity)

    @Query("SELECT * FROM inventory ORDER BY boughtAt DESC")
    fun all(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE id = :id")
    suspend fun byId(id: Long): InventoryEntity?
}
