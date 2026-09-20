package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BlockLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockLogDao {
    @Query("SELECT * FROM block_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<BlockLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BlockLog): Long

    @Query("DELETE FROM block_logs WHERE timestamp < :olderThan")
    suspend fun cleanOldLogs(olderThan: Long)

    @Query("DELETE FROM block_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM block_logs")
    fun getBlockedCount(): Flow<Int>
}
