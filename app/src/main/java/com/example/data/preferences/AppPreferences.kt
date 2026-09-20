package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_guard_settings")

class AppPreferences(private val context: Context) {
    companion object {
        private val KEY_VPN_ENABLED = booleanPreferencesKey("vpn_enabled")
        private val KEY_GLOBAL_INTERNET_LOCK = booleanPreferencesKey("global_internet_lock")
        private val KEY_ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        private val KEY_PAUSED_UNTIL = longPreferencesKey("paused_until")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_ACCENT_COLOR_INDEX = intPreferencesKey("accent_color_index")
        private val KEY_AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        private val KEY_NOTIFY_BLOCKED_ATTEMPTS = booleanPreferencesKey("notify_blocked_attempts")
        private val KEY_NOTIFY_THRESHOLD_ALERTS = booleanPreferencesKey("notify_threshold_alerts")
    }

    val isVpnEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_VPN_ENABLED] ?: false }
    val isGlobalInternetLock: Flow<Boolean> = context.dataStore.data.map { it[KEY_GLOBAL_INTERNET_LOCK] ?: false }
    val activeProfile: Flow<String> = context.dataStore.data.map { it[KEY_ACTIVE_PROFILE] ?: "NORMAL" }
    val pausedUntil: Flow<Long> = context.dataStore.data.map { it[KEY_PAUSED_UNTIL] ?: 0L }
    val themeMode: Flow<String> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: "SYSTEM" }
    val accentColorIndex: Flow<Int> = context.dataStore.data.map { it[KEY_ACCENT_COLOR_INDEX] ?: 0 }
    val autoStartOnBoot: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_START_BOOT] ?: true }
    val notifyBlockedAttempts: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFY_BLOCKED_ATTEMPTS] ?: true }
    val notifyThresholdAlerts: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFY_THRESHOLD_ALERTS] ?: true }

    suspend fun setVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VPN_ENABLED] = enabled }
    }

    suspend fun setGlobalInternetLock(locked: Boolean) {
        context.dataStore.edit { it[KEY_GLOBAL_INTERNET_LOCK] = locked }
    }

    suspend fun setActiveProfile(profile: String) {
        context.dataStore.edit { it[KEY_ACTIVE_PROFILE] = profile }
    }

    suspend fun setPausedUntil(untilTimestamp: Long) {
        context.dataStore.edit { it[KEY_PAUSED_UNTIL] = untilTimestamp }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setAccentColorIndex(index: Int) {
        context.dataStore.edit { it[KEY_ACCENT_COLOR_INDEX] = index }
    }

    suspend fun setAutoStartOnBoot(autoStart: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_START_BOOT] = autoStart }
    }

    suspend fun setNotifyBlockedAttempts(notify: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_BLOCKED_ATTEMPTS] = notify }
    }

    suspend fun setNotifyThresholdAlerts(notify: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_THRESHOLD_ALERTS] = notify }
    }
}
