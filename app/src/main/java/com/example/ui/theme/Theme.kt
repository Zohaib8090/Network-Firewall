package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BaseLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = ShieldSecondary,
    tertiary = ShieldTertiary,
    background = SurfaceLight,
    surface = SurfaceLight,
    error = ErrorRed
)

private val BaseDarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkPrimaryContainer,
    onPrimaryContainer = EmeraldDarkPrimaryContainer,
    secondary = ShieldDarkSecondary,
    tertiary = ShieldDarkTertiary,
    background = SurfaceDark,
    surface = SurfaceDark,
    error = ErrorRedDark
)

private val OledColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003825),
    onPrimaryContainer = Color(0xFF8CF8C7),
    secondary = ShieldDarkSecondary,
    tertiary = ShieldDarkTertiary,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF121212),
    error = ErrorRedDark
)

@Composable
fun SmartNetworkGuardTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK", "OLED"
    accentIndex: Int = 0, // 0 = dynamic/default, 1=Emerald, 2=Indigo, 3=Coral, 4=Cyan, 5=Amber
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK", "OLED" -> true
        else -> systemDark
    }

    val isOled = themeMode == "OLED"

    val baseScheme: ColorScheme = when {
        isOled -> OledColorScheme
        accentIndex == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> BaseDarkColorScheme
        else -> BaseLightColorScheme
    }

    // Apply custom accent color if selected
    val finalScheme = if (accentIndex > 0) {
        val accent = when (accentIndex) {
            1 -> AccentEmerald
            2 -> AccentIndigo
            3 -> AccentCoral
            4 -> AccentCyan
            5 -> AccentAmber
            else -> EmeraldPrimary
        }
        if (isDark) {
            baseScheme.copy(primary = accent, secondary = accent.copy(alpha = 0.8f))
        } else {
            baseScheme.copy(primary = accent, primaryContainer = accent.copy(alpha = 0.2f))
        }
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}
