package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppRule

@Composable
fun DataLimitDialog(
    rule: AppRule,
    onSaveLimits: (dailyBytes: Long, weeklyBytes: Long, monthlyBytes: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialDailyMb = if (rule.dailyLimitBytes > 0) (rule.dailyLimitBytes / (1024 * 1024)).toString() else ""
    val initialMonthlyMb = if (rule.monthlyLimitBytes > 0) (rule.monthlyLimitBytes / (1024 * 1024)).toString() else ""

    var dailyMbText by remember { mutableStateOf(initialDailyMb) }
    var monthlyMbText by remember { mutableStateOf(initialMonthlyMb) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Data Cap: ${rule.appName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Alerts are sent at 80%, 90%, and 100% consumption. When reaching 100%, network traffic can be automatically stopped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dailyMbText,
                    onValueChange = { dailyMbText = it },
                    label = { Text("Daily Limit (MB)") },
                    placeholder = { Text("e.g. 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_daily_limit")
                )

                OutlinedTextField(
                    value = monthlyMbText,
                    onValueChange = { monthlyMbText = it },
                    label = { Text("Monthly Limit (MB)") },
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_monthly_limit")
                )

                // Quick presets
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { dailyMbText = "250" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("250 MB")
                    }
                    OutlinedButton(
                        onClick = { dailyMbText = "1024" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1 GB")
                    }
                    OutlinedButton(
                        onClick = { dailyMbText = "2048" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("2 GB")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dailyBytes = (dailyMbText.toLongOrNull() ?: 0L) * 1024L * 1024L
                    val monthlyBytes = (monthlyMbText.toLongOrNull() ?: 0L) * 1024L * 1024L
                    onSaveLimits(dailyBytes, 0L, monthlyBytes)
                },
                modifier = Modifier.testTag("btn_save_limits")
            ) {
                Text("Save Limits")
            }
        },
        dismissButton = {
            Row {
                if (rule.dailyLimitBytes > 0 || rule.monthlyLimitBytes > 0) {
                    TextButton(
                        onClick = { onSaveLimits(0L, 0L, 0L) }
                    ) {
                        Text("Remove Limits", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
