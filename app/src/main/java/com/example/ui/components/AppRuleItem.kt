package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onTogglePin: (isPinned: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val rule = appUsage.rule
    val isWifiBlocked = rule.isWifiBlocked
    val isMobileBlocked = rule.isMobileBlocked
    val now = System.currentTimeMillis()
    val isTempAllowed = rule.temporaryAccessUntil > now
    val isSessionAllowed = rule.allowSession
    val isFullyBlocked = isWifiBlocked && isMobileBlocked

    // Left border accent color based on status
    val accentColor by animateColorAsState(
        targetValue = when {
            isFullyBlocked -> StatusBlocked
            isWifiBlocked || isMobileBlocked -> StatusPaused
            isTempAllowed || isSessionAllowed -> StatusAllowed.copy(alpha = 0.7f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "accent_color"
    )

    val cardElevation by animateDpAsState(
        targetValue = if (isExpanded) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "card_elevation"
    )

    val openApp = {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(appUsage.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "${appUsage.appName} cannot be opened directly", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_item_${appUsage.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFullyBlocked -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                isExpanded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Animated left accent stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isExpanded) 140.dp else 72.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            Column(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)) {
                // Main row — always visible
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
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

                    // App Name & Stats
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = appUsage.appName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (appUsage.isSystemApp) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                ) {
                                    Text(
                                        text = "SYS",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (rule.isPinned) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = "Today: ${appUsage.totalFormattedToday}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Status badges
                        if (isTempAllowed || isSessionAllowed || rule.dailyLimitBytes > 0L) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(top = 3.dp)
                            ) {
                                if (isTempAllowed) {
                                    val remainingMin = ((rule.temporaryAccessUntil - now) / 60000L).coerceAtLeast(1)
                                    StatusBadge(
                                        text = "${remainingMin}m pass",
                                        color = StatusPaused,
                                        icon = { Icon(Icons.Default.HourglassTop, null, modifier = Modifier.size(10.dp), tint = StatusPaused) }
                                    )
                                } else if (isSessionAllowed) {
                                    StatusBadge(text = "Session", color = StatusAllowed)
                                }
                                if (rule.dailyLimitBytes > 0L) {
                                    StatusBadge(
                                        text = "Cap: ${AppUsageInfo.formatBytes(rule.dailyLimitBytes)}/day",
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // WiFi pill toggle
                    NetworkPillToggle(
                        isBlocked = isWifiBlocked,
                        blockedIcon = Icons.Default.WifiOff,
                        allowedIcon = Icons.Default.Wifi,
                        blockedColor = StatusBlocked,
                        allowedColor = WifiBlue,
                        label = if (isWifiBlocked) "Wi-Fi" else "Wi-Fi",
                        onToggle = { onToggleWifi(!isWifiBlocked) },
                        testTag = "wifi_toggle_${appUsage.packageName}"
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Mobile pill toggle
                    NetworkPillToggle(
                        isBlocked = isMobileBlocked,
                        blockedIcon = Icons.Default.Block,
                        allowedIcon = Icons.Default.SignalCellularAlt,
                        blockedColor = StatusBlocked,
                        allowedColor = MobileOrange,
                        label = "Cell",
                        onToggle = { onToggleMobile(!isMobileBlocked) },
                        testTag = "mobile_toggle_${appUsage.packageName}"
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Expand indicator
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expanded actions panel
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(160))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        // Usage breakdown
                        Text(
                            text = "Wi-Fi: ${appUsage.wifiFormattedToday}  ·  Cell: ${appUsage.mobileFormattedToday}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Action buttons in two rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButton(
                                onClick = { onTempAccessClick(10); isExpanded = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.HourglassTop, null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("10m Pass", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = { onTempAccessClick(30); isExpanded = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.HourglassTop, null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("30m Pass", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = { onAllowSessionClick(); isExpanded = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Session", fontSize = 11.sp)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButton(
                                onClick = { onSetLimitClick(); isExpanded = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Speed, null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Data Limit", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = { onTogglePin(!rule.isPinned); isExpanded = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PushPin, null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (rule.isPinned) "Unpin" else "Pin Top", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = { openApp(); isExpanded = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Launch, null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Open", fontSize = 11.sp)
                            }
                        }
                        TextButton(
                            onClick = { onResetRuleClick(); isExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset to Default (Allow All)", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkPillToggle(
    isBlocked: Boolean,
    blockedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    allowedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    blockedColor: Color,
    allowedColor: Color,
    label: String,
    onToggle: () -> Unit,
    testTag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (isBlocked) blockedColor.copy(alpha = 0.15f) else allowedColor.copy(alpha = 0.13f),
        animationSpec = tween(220),
        label = "pill_bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isBlocked) blockedColor else allowedColor,
        animationSpec = tween(220),
        label = "pill_icon"
    )

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = Modifier
            .size(width = 46.dp, height = 38.dp)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isBlocked) blockedIcon else allowedIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 8.sp,
                color = iconColor,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 10.sp
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    icon: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            icon?.invoke()
            Text(text = text, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
