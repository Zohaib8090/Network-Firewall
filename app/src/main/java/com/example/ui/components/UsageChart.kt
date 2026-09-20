package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageInfo
import com.example.data.repository.DayUsageData
import com.example.data.repository.SpeedMetrics
import com.example.ui.theme.MobileOrange
import com.example.ui.theme.WifiBlue

@Composable
fun RealtimeSpeedCard(
    metrics: SpeedMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_speed_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Live Throughput",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "REAL-TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Download Speed
                SpeedIndicator(
                    icon = Icons.Default.ArrowDownward,
                    iconColor = WifiBlue,
                    label = "Download",
                    speedBytesPerSec = metrics.rxBytesPerSec,
                    totalBytes = metrics.totalRxBytes
                )

                // Upload Speed
                SpeedIndicator(
                    icon = Icons.Default.ArrowUpward,
                    iconColor = MobileOrange,
                    label = "Upload",
                    speedBytesPerSec = metrics.txBytesPerSec,
                    totalBytes = metrics.totalTxBytes
                )
            }
        }
    }
}

@Composable
private fun SpeedIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    speedBytesPerSec: Long,
    totalBytes: Long
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatRate(speedBytesPerSec),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total: ${AppUsageInfo.formatBytes(totalBytes)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRate(bytesPerSec: Long): String {
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.2f MB/s", mb)
        kb >= 1.0 -> String.format("%.1f KB/s", kb)
        else -> "$bytesPerSec B/s"
    }
}

@Composable
fun UsageHistoryBarChart(
    daysData: List<DayUsageData>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val maxBytes = (daysData.maxOfOrNull { it.totalBytes } ?: 1L).coerceAtLeast(1024L * 1024L)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("usage_history_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "7-Day Usage Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(WifiBlue, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wi-Fi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(8.dp).background(MobileOrange, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mobile", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Selected details prompt
            if (selectedIndex in daysData.indices) {
                val item = daysData[selectedIndex]
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${item.dayLabel}: Total ${AppUsageInfo.formatBytes(item.totalBytes)} (Wi-Fi: ${AppUsageInfo.formatBytes(item.wifiBytes)}, Mobile: ${AppUsageInfo.formatBytes(item.mobileBytes)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap a day column to inspect details",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(daysData) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / daysData.size
                            val tapped = (offset.x / slotWidth).toInt().coerceIn(0, daysData.size - 1)
                            selectedIndex = tapped
                        }
                    }
            ) {
                val chartHeight = size.height - 30.dp.toPx()
                val chartWidth = size.width
                val barCount = daysData.size
                val slotWidth = chartWidth / barCount
                val barWidth = slotWidth * 0.48f

                // Draw horizontal grid lines
                for (i in 1..3) {
                    val y = chartHeight * (i / 4f)
                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw Bars
                daysData.forEachIndexed { index, day ->
                    val xCenter = (index * slotWidth) + (slotWidth / 2f)
                    val barLeft = xCenter - (barWidth / 2f)

                    val wifiRatio = (day.wifiBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
                    val mobileRatio = (day.mobileBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
                    val totalRatio = ((day.wifiBytes + day.mobileBytes).toFloat() / maxBytes).coerceIn(0.04f, 1f)

                    val totalBarHeight = chartHeight * totalRatio
                    val mobileBarHeight = chartHeight * mobileRatio
                    val wifiBarHeight = (totalBarHeight - mobileBarHeight).coerceAtLeast(0f)

                    val isSelected = index == selectedIndex

                    // Draw Wi-Fi Portion (Bottom)
                    val wifiY = chartHeight - wifiBarHeight
                    drawRoundRect(
                        color = if (isSelected) WifiBlue else WifiBlue.copy(alpha = 0.85f),
                        topLeft = Offset(barLeft, wifiY),
                        size = Size(barWidth, wifiBarHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw Mobile Portion (Stacked on top)
                    val mobileY = wifiY - mobileBarHeight
                    if (mobileBarHeight > 0) {
                        drawRoundRect(
                            color = if (isSelected) MobileOrange else MobileOrange.copy(alpha = 0.85f),
                            topLeft = Offset(barLeft, mobileY),
                            size = Size(barWidth, mobileBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Selected ring
                    if (isSelected) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(barLeft - 2.dp.toPx(), mobileY - 2.dp.toPx()),
                            size = Size(barWidth + 4.dp.toPx(), totalBarHeight + 4.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysData.forEachIndexed { index, day ->
                    Text(
                        text = day.dayLabel,
                        fontSize = 11.sp,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
