package dev.zohaib.networkfirewall.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zohaib.networkfirewall.data.update.UpdateResult
import dev.zohaib.networkfirewall.data.update.UpdateUiState

/** Settings card with a "Check for updates" button and the result of the last check. */
@Composable
fun UpdateCard(
    currentVersion: String,
    state: UpdateUiState,
    onCheck: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("update_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "App updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Installed version $currentVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state is UpdateUiState.Done) {
                UpdateResultView(result = state.result, currentVersion = currentVersion, onOpenUrl = onOpenUrl)
            }

            Button(
                onClick = onCheck,
                enabled = state !is UpdateUiState.Checking,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_check_updates")
            ) {
                if (state is UpdateUiState.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking…")
                } else {
                    Text("Check for updates")
                }
            }

            Text(
                text = "Asks GitHub for the newest release when you tap the button. No information about you or your apps is sent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UpdateResultView(
    result: UpdateResult,
    currentVersion: String,
    onOpenUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("update_result"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (result) {
            is UpdateResult.UpToDate -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You're on the latest version ($currentVersion).",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is UpdateResult.UpdateAvailable -> {
                Text(
                    text = "Version ${result.version} is available",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (result.notes.isNotBlank()) {
                    Text(
                        text = result.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onOpenUrl(result.downloadUrl ?: result.releaseUrl) },
                        modifier = Modifier.testTag("btn_download_update")
                    ) {
                        Text(if (result.downloadUrl != null) "Download" else "Open release")
                    }
                    if (result.downloadUrl != null) {
                        OutlinedButton(
                            onClick = { onOpenUrl(result.releaseUrl) },
                            modifier = Modifier.testTag("btn_release_page")
                        ) {
                            Text("Release page")
                        }
                    }
                }
            }

            UpdateResult.NoReleases -> Text(
                text = "No release has been published yet.",
                style = MaterialTheme.typography.bodyMedium
            )

            is UpdateResult.Failed -> Text(
                text = result.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
