package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY isPinned DESC, appName ASC")
    fun getAllRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleByPackage(packageName: String): AppRule?

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    fun getRuleFlow(packageName: String): Flow<AppRule?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<AppRule>)

    @Update
    suspend fun updateRule(rule: AppRule)

    @Query("UPDATE app_rules SET isWifiBlocked = :blockWifi, isMobileBlocked = :blockMobile WHERE packageName = :packageName")
    suspend fun updateToggles(packageName: String, blockWifi: Boolean, blockMobile: Boolean)

    @Query("UPDATE app_rules SET isPinned = :isPinned WHERE packageName = :packageName")
    suspend fun setPinned(packageName: String, isPinned: Boolean)

    @Query("UPDATE app_rules SET temporaryAccessUntil = :untilTime WHERE packageName = :packageName")
    suspend fun setTemporaryAccess(packageName: String, untilTime: Long)

    @Query("UPDATE app_rules SET allowSession = :allow WHERE packageName = :packageName")
    suspend fun setAllowSession(packageName: String, allow: Boolean)

    @Query("UPDATE app_rules SET isWifiBlocked = :blockWifi, isMobileBlocked = :blockMobile")
    suspend fun setAllToggles(blockWifi: Boolean, blockMobile: Boolean)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("DELETE FROM app_rules")
    suspend fun clearAllRules()
}
