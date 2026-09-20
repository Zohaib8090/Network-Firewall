package com.example.data.model

enum class ProfileType(val displayName: String, val description: String) {
    NORMAL("Normal", "Standard personalized app rules"),
    WORK_STUDY("Study / Work", "Blocks social media, games, and streaming distractors"),
    GAMING("Gaming", "Restricts background sync and heavy downloaders to minimize lag"),
    BATTERY_SAVER("Battery Saver", "Disables network for power-hungry background apps"),
    OFFLINE_MODE("Offline Mode", "Complete device-wide network isolation")
}

data class ProfilePreset(
    val type: ProfileType,
    val isWifiBlockedDefault: Boolean = false,
    val isMobileBlockedDefault: Boolean = false,
    val targetCategories: Set<String> = emptySet(),
    val iconName: String = "shield"
)
