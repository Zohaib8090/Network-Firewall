package dev.zohaib.networkfirewall.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.zohaib.networkfirewall.data.model.AppRule
import dev.zohaib.networkfirewall.data.model.BlockLog
import dev.zohaib.networkfirewall.data.model.ScheduleRule

@Database(
    entities = [
        AppRule::class,
        ScheduleRule::class,
        BlockLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun blockLogDao(): BlockLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_network_guard.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
