package com.example.airecipeapp.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.airecipeapp.data.models.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scanHistory: ScanHistoryEntity): Long
    
    @Update
    suspend fun update(scanHistory: ScanHistoryEntity)
    
    @Delete
    suspend fun delete(scanHistory: ScanHistoryEntity)
    
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanHistoryEntity>>
    
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScans(limit: Int = 10): Flow<List<ScanHistoryEntity>>
    
    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getScanById(id: Long): ScanHistoryEntity?
    
    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScanById(id: Long)
    
    @Query("SELECT COUNT(*) FROM scan_history")
    fun getScanCount(): Flow<Int>
    
    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()
}
