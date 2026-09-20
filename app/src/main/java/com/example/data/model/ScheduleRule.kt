package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_rules")
data class ScheduleRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val startHour: Int, // 0 - 23
    val startMinute: Int, // 0 - 59
    val endHour: Int, // 0 - 23
    val endMinute: Int, // 0 - 59
    val daysOfWeek: String, // Comma-separated: "MON,TUE,WED,THU,FRI,SAT,SUN"
    val targetPackageNames: String, // Comma-separated or "ALL"
    val blockWifi: Boolean = true,
    val blockMobile: Boolean = true,
    val isEnabled: Boolean = true
) {
    fun isActiveAt(calendarHour: Int, calendarMinute: Int, dayCode: String): Boolean {
        if (!isEnabled) return false
        val activeDays = daysOfWeek.split(",").map { it.trim().uppercase() }
        if (!activeDays.contains(dayCode)) return false

        val currentMinutes = calendarHour * 60 + calendarMinute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnights (e.g. 23:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
