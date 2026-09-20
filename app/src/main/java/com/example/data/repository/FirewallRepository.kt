package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.db.AppDatabase
import com.example.data.model.AppRule
import com.example.data.model.BlockLog
import com.example.data.model.ProfileType
import com.example.data.model.ScheduleRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class FirewallRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val appRuleDao = database.appRuleDao()
    private val scheduleRuleDao = database.scheduleRuleDao()
    private val blockLogDao = database.blockLogDao()

    val allRules: Flow<List<AppRule>> = appRuleDao.getAllRules()
    val allSchedules: Flow<List<ScheduleRule>> = scheduleRuleDao.getAllSchedules()
    val recentBlockLogs: Flow<List<BlockLog>> = blockLogDao.getRecentLogs(100)
    val blockedCount: Flow<Int> = blockLogDao.getBlockedCount()

    suspend fun getRuleByPackage(packageName: String): AppRule? {
        return appRuleDao.getRuleByPackage(packageName)
    }

    suspend fun updateToggles(packageName: String, blockWifi: Boolean, blockMobile: Boolean) {
        appRuleDao.updateToggles(packageName, blockWifi, blockMobile)
    }

    suspend fun saveRule(rule: AppRule) {
        appRuleDao.insertRule(rule)
    }

    suspend fun setTemporaryAccess(packageName: String, durationMinutes: Int) {
        val until = if (durationMinutes > 0) {
            System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        } else 0L
        appRuleDao.setTemporaryAccess(packageName, until)
    }

    suspend fun setAllowSession(packageName: String, allow: Boolean) {
        appRuleDao.setAllowSession(packageName, allow)
    }

    suspend fun updateDataLimit(packageName: String, dailyBytes: Long, weeklyBytes: Long, monthlyBytes: Long) {
        val existing = appRuleDao.getRuleByPackage(packageName)
        if (existing != null) {
            val updated = existing.copy(
                dailyLimitBytes = dailyBytes,
                weeklyLimitBytes = weeklyBytes,
                monthlyLimitBytes = monthlyBytes
            )
            appRuleDao.updateRule(updated)
        }
    }

    suspend fun insertBlockLog(log: BlockLog) {
        blockLogDao.insertLog(log)
    }

    suspend fun clearBlockLogs() {
        blockLogDao.clearAllLogs()
    }

    suspend fun addSchedule(schedule: ScheduleRule): Long {
        return scheduleRuleDao.insertSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: ScheduleRule) {
        scheduleRuleDao.updateSchedule(schedule)
    }

    suspend fun setScheduleEnabled(id: Long, enabled: Boolean) {
        scheduleRuleDao.setScheduleEnabled(id, enabled)
    }

    suspend fun deleteSchedule(id: Long) {
        scheduleRuleDao.deleteSchedule(id)
    }

    suspend fun resetAllRules() {
        appRuleDao.clearAllRules()
        syncInstalledApps()
    }

    suspend fun setAllBlocked(blockWifi: Boolean, blockMobile: Boolean) {
        appRuleDao.setAllToggles(blockWifi, blockMobile)
    }

    suspend fun applyProfile(profileType: ProfileType) = withContext(Dispatchers.IO) {
        when (profileType) {
            ProfileType.NORMAL -> {
                // Restore unblocked state for non-custom rules
                val pm = context.packageManager
                val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (app in installed) {
                    val existing = appRuleDao.getRuleByPackage(app.packageName)
                    if (existing == null) {
                        val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        appRuleDao.insertRule(
                            AppRule(
                                packageName = app.packageName,
                                appName = pm.getApplicationLabel(app).toString(),
                                isWifiBlocked = false,
                                isMobileBlocked = false,
                                isSystemApp = isSystem
                            )
                        )
                    }
                }
            }
            ProfileType.WORK_STUDY -> {
                // Block distracting categories / apps (social, media, games)
                val distractors = setOf(
                    "com.facebook.katana", "com.instagram.android", "com.tiktok",
                    "com.twitter.android", "com.zhiliaoapp.musically", "com.snapchat.android",
                    "com.google.android.youtube", "com.netflix.mediaclient", "com.reddit.frontpage",
                    "tv.twitch.android.app", "com.spotify.music", "com.pinterest"
                )
                val currentRules = appRuleDao.getRuleByPackage("dummy") // trigger query
                val pm = context.packageManager
                val installed = pm.getInstalledApplications(0)
                for (app in installed) {
                    val name = pm.getApplicationLabel(app).toString().lowercase()
                    val pkg = app.packageName.lowercase()
                    val shouldBlock = distractors.any { pkg.contains(it) } ||
                            name.contains("tiktok") || name.contains("instagram") ||
                            name.contains("game") || name.contains("youtube") || name.contains("reddit")

                    if (shouldBlock) {
                        appRuleDao.updateToggles(app.packageName, blockWifi = true, blockMobile = true)
                    }
                }
            }
            ProfileType.GAMING -> {
                // Block non-essential background downloaders / cloud sync to lower latency
                val backgroundHogs = setOf("com.google.android.apps.photos", "com.dropbox.android", "com.google.android.apps.docs")
                val pm = context.packageManager
                val installed = pm.getInstalledApplications(0)
                for (app in installed) {
                    if (backgroundHogs.contains(app.packageName) || app.packageName.contains("cloud") || app.packageName.contains("sync")) {
                        appRuleDao.updateToggles(app.packageName, blockWifi = true, blockMobile = true)
                    }
                }
            }
            ProfileType.BATTERY_SAVER -> {
                // Block heavy mobile data & wifi for power draining background apps
                val pm = context.packageManager
                val installed = pm.getInstalledApplications(0)
                for (app in installed) {
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!isSystem && app.packageName != context.packageName) {
                        appRuleDao.updateToggles(app.packageName, blockWifi = false, blockMobile = true)
                    }
                }
            }
            ProfileType.OFFLINE_MODE -> {
                // Block all non-system apps
                val pm = context.packageManager
                val installed = pm.getInstalledApplications(0)
                for (app in installed) {
                    if (app.packageName != context.packageName) {
                        appRuleDao.updateToggles(app.packageName, blockWifi = true, blockMobile = true)
                    }
                }
            }
        }
    }

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val newRules = mutableListOf<AppRule>()
        for (app in installed) {
            // Do not firewall our own app
            if (app.packageName == context.packageName) continue
            val existing = appRuleDao.getRuleByPackage(app.packageName)
            if (existing == null) {
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = try {
                    pm.getApplicationLabel(app).toString()
                } catch (e: Exception) {
                    app.packageName
                }
                newRules.add(
                    AppRule(
                        packageName = app.packageName,
                        appName = label,
                        isWifiBlocked = false,
                        isMobileBlocked = false,
                        isSystemApp = isSystem
                    )
                )
            }
        }
        if (newRules.isNotEmpty()) {
            appRuleDao.insertRules(newRules)
        }
    }

    suspend fun exportRulesJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val rulesArray = JSONArray()
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(0)
        for (app in installed) {
            val rule = appRuleDao.getRuleByPackage(app.packageName)
            if (rule != null && (rule.isWifiBlocked || rule.isMobileBlocked || rule.dailyLimitBytes > 0)) {
                val obj = JSONObject().apply {
                    put("packageName", rule.packageName)
                    put("appName", rule.appName)
                    put("isWifiBlocked", rule.isWifiBlocked)
                    put("isMobileBlocked", rule.isMobileBlocked)
                    put("dailyLimitBytes", rule.dailyLimitBytes)
                    put("weeklyLimitBytes", rule.weeklyLimitBytes)
                    put("monthlyLimitBytes", rule.monthlyLimitBytes)
                }
                rulesArray.put(obj)
            }
        }
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())
        root.put("rules", rulesArray)
        root.toString(2)
    }

    suspend fun importRulesJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val rulesArray = root.getJSONArray("rules")
            var count = 0
            for (i in 0 until rulesArray.length()) {
                val obj = rulesArray.getJSONObject(i)
                val pkg = obj.getString("packageName")
                val rule = AppRule(
                    packageName = pkg,
                    appName = obj.optString("appName", pkg),
                    isWifiBlocked = obj.optBoolean("isWifiBlocked", false),
                    isMobileBlocked = obj.optBoolean("isMobileBlocked", false),
                    dailyLimitBytes = obj.optLong("dailyLimitBytes", 0L),
                    weeklyLimitBytes = obj.optLong("weeklyLimitBytes", 0L),
                    monthlyLimitBytes = obj.optLong("monthlyLimitBytes", 0L)
                )
                appRuleDao.insertRule(rule)
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
