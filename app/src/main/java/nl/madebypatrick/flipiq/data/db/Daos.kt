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
