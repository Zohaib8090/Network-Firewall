package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.AppUsageInfo
import com.example.ui.theme.MobileOrange
import com.example.ui.theme.StatusAllowed
import com.example.ui.theme.StatusBlocked
import com.example.ui.theme.StatusPaused
import com.example.ui.theme.WifiBlue

@Composable
fun AppRuleItem(
    appUsage: AppUsageInfo,
    onToggleWifi: (blocked: Boolean) -> Unit,
    onToggleMobile: (blocked: Boolean) -> Unit,
    onSetLimitClick: () -> Unit,
    onTempAccessClick: (minutes: Int) -> Unit,
    onAllowSessionClick: () -> Unit,
    onResetRuleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val rule = appUsage.rule
    val isWifiBlocked = rule.isWifiBlocked
    val isMobileBlocked = rule.isMobileBlocked
    val now = System.currentTimeMillis()
    val isTempAllowed = rule.temporaryAccessUntil > now
    val isSessionAllowed = rule.allowSession

    val openApp = {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(appUsage.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "${appUsage.appName} cannot be opened directly", Toast.LENGTH_SHORT).show()
        }
    }

    val cardColor = if (isWifiBlocked && isMobileBlocked) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { openApp() }
            .testTag("app_item_${appUsage.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App Icon
                if (appUsage.icon != null) {
                    val bitmap = remember(appUsage.icon) {
                        appUsage.icon.toBitmap(width = 96, height = 96)
                    }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = appUsage.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appUsage.appName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App Name and Package/Stats
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appUsage.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (appUsage.isSystemApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "SYSTEM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "Today: ${appUsage.totalFormattedToday} (Wi-Fi: ${appUsage.wifiFormattedToday}, Cell: ${appUsage.mobileFormattedToday})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Badges for temporary access or limit
                    if (isTempAllowed || isSessionAllowed || rule.dailyLimitBytes > 0L) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (isTempAllowed) {
                                val remainingMin = ((rule.temporaryAccessUntil - now) / 60000L).coerceAtLeast(1)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StatusPaused.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = StatusPaused,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${remainingMin}m temp pass",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = StatusPaused
                                        )
                                    }
                                }
                            } else if (isSessionAllowed) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StatusAllowed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Session allowed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StatusAllowed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (rule.dailyLimitBytes > 0L) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "Cap: ${AppUsageInfo.formatBytes(rule.dailyLimitBytes)}/day",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Wi-Fi Toggle Button
                FilledIconToggleButton(
                    checked = !isWifiBlocked,
                    onCheckedChange = { allowed -> onToggleWifi(!allowed) },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("wifi_toggle_${appUsage.packageName}"),
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        checkedContainerColor = WifiBlue.copy(alpha = 0.15f),
                        checkedContentColor = WifiBlue,
                        containerColor = StatusBlocked.copy(alpha = 0.15f),
                        contentColor = StatusBlocked
                    )
                ) {
                    Icon(
                        imageVector = if (!isWifiBlocked) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = if (isWifiBlocked) "Wi-Fi Blocked" else "Wi-Fi Allowed",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Mobile Data Toggle Button
                FilledIconToggleButton(
                    checked = !isMobileBlocked,
                    onCheckedChange = { allowed -> onToggleMobile(!allowed) },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("mobile_toggle_${appUsage.packageName}"),
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        checkedContainerColor = MobileOrange.copy(alpha = 0.15f),
                        checkedContentColor = MobileOrange,
                        containerColor = StatusBlocked.copy(alpha = 0.15f),
                        contentColor = StatusBlocked
                    )
                ) {
                    Icon(
                        imageVector = if (!isMobileBlocked) Icons.Default.SignalCellularAlt else Icons.Default.Block,
                        contentDescription = if (isMobileBlocked) "Mobile Data Blocked" else "Mobile Data Allowed",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // More Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "App options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open App") },
                            onClick = {
                                showMenu = false
                                openApp()
                            },
                            leadingIcon = { Icon(Icons.Default.Launch, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Set Data Limit") },
                            onClick = {
                                showMenu = false
                                onSetLimitClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Temporary Pass (10 min)") },
                            onClick = {
                                showMenu = false
                                onTempAccessClick(10)
                            },
                            leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Temporary Pass (30 min)") },
                            onClick = {
                                showMenu = false
                                onTempAccessClick(30)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Allow for this session") },
                            onClick = {
                                showMenu = false
                                onAllowSessionClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reset to default (Allow All)") },
                            onClick = {
                                showMenu = false
                                onResetRuleClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
