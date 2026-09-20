package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScheduleRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleRuleDao {
    @Query("SELECT * FROM schedule_rules ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedules(): Flow<List<ScheduleRule>>

    @Query("SELECT * FROM schedule_rules WHERE isEnabled = 1")
    suspend fun getActiveSchedules(): List<ScheduleRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleRule): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleRule)

    @Query("UPDATE schedule_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setScheduleEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM schedule_rules WHERE id = :id")
    suspend fun deleteSchedule(id: Long)

    @Query("DELETE FROM schedule_rules")
    suspend fun clearAllSchedules()
}
