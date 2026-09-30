package dev.zohaib.networkfirewall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.zohaib.networkfirewall.data.model.BlockLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockLogDao {
    @Query("SELECT * FROM block_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<BlockLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BlockLog): Long

    /** Deletes everything except the [keep] newest entries, so the table can't grow without bound. */
    @Query("DELETE FROM block_logs WHERE id NOT IN (SELECT id FROM block_logs ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun keepNewest(keep: Int)

    @Query("DELETE FROM block_logs WHERE timestamp < :olderThan")
    suspend fun cleanOldLogs(olderThan: Long)

    @Query("DELETE FROM block_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM block_logs")
    fun getBlockedCount(): Flow<Int>
}
