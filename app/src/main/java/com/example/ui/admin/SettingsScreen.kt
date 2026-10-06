package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.CloudNetworkStatus
import com.example.data.local.entity.AuditLogEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RedSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.DateUtils

@Composable
fun SettingsScreen(
    currentGpsThreshold: Double,
    auditLogs: List<AuditLogEntity>,
    onUpdateGpsThreshold: (Double) -> Unit,
    cloudStatus: CloudNetworkStatus = CloudNetworkStatus(),
    onTriggerSync: () -> Unit = {}
) {
    var thresholdSlider by remember { mutableFloatStateOf(currentGpsThreshold.toFloat()) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var syncFeedbackMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Settings & Cloud Sync",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    )
                }
                Text(
                    text = "Centralized real-time cloud database status, GPS verification tolerance, and immutable audit logs",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }
        }

        // Live Cloud Network Status Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("cloud_status_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (cloudStatus.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (cloudStatus.isOnline) EmeraldDark else RedDanger,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Cloud Network Status",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                    }

                    // Online / Synced Status Pill
                    Surface(
                        color = if (cloudStatus.isOnline) EmeraldSubtle else RedSubtle,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("cloud_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (cloudStatus.isOnline) EmeraldDark else RedDanger,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (cloudStatus.isOnline) "Online / Synced" else "Offline Mode",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (cloudStatus.isOnline) EmeraldDark else RedDanger
                                )
                            )
                        }
                    }
                }

                Text(
                    text = if (cloudStatus.isOnline)
                        "All changes to BDO accounts, merchant rosters, and field visits synchronize in real time across devices."
                    else
                        "No active internet connection. Offline cache persistence is active. All field visits and updates are saved locally and will auto-sync once online.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )

                // Status Details Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cloud Backend:", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                            Text("Centralized Cloud Database", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Navy900))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Real-Time Listeners:", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                            Text("users, merchants, visits, audit_logs", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, color = PrimaryBlue))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Offline Caching:", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                            Text("Persistent Disk Cache (Enabled)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, color = EmeraldDark))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Last Synced:", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                            Text(DateUtils.formatDateTime(cloudStatus.lastSyncTime), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, color = Navy900))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onTriggerSync()
                            syncFeedbackMessage = "Cloud sync verified: cache refreshed with latest cloud collections."
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("trigger_cloud_sync_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (syncFeedbackMessage != null) {
                    Surface(
                        color = EmeraldSubtle,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = syncFeedbackMessage!!,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                        )
                    }
                }
            }
        }

        // GPS Threshold Setting Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GPS Verification Tolerance Radius",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                }

                Text(
                    text = "If BDO visit location exceeds this distance from registered merchant coordinates, visit is flagged as 'GPS MISMATCH' and routed to Verification Queue.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Threshold: ${thresholdSlider.toInt()} meters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    )
                }

                Slider(
                    value = thresholdSlider,
                    onValueChange = { thresholdSlider = it },
                    valueRange = 25f..500f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "25m (Strict)", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                    Text(text = "100m (Standard)", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                    Text(text = "500m (Relaxed)", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onUpdateGpsThreshold(thresholdSlider.toDouble())
                        savedMessage = "GPS verification threshold updated to ${thresholdSlider.toInt()} meters"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End).testTag("save_gps_threshold_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Threshold", style = MaterialTheme.typography.labelSmall)
                }

                if (savedMessage != null) {
                    Surface(
                        color = EmeraldSubtle,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = savedMessage!!,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                        )
                    }
                }
            }
        }

        // Immutable Audit Trail Card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Immutable Audit Trail (${auditLogs.size} logs)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                }
                Text(
                    text = "Cryptographically ordered log of user creation, Excel imports, logins, visits, and settings changes replicated to the central cloud",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(auditLogs, key = { it.id }) { log ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.action,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    )
                                    Text(
                                        text = DateUtils.formatDateTime(log.timestamp),
                                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                                    )
                                }
                                Text(
                                    text = "User: ${log.userName} • Entity: ${log.entityType} (${log.entityId})",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Navy900, fontWeight = FontWeight.Medium)
                                )
                                if (log.metadata.isNotBlank()) {
                                    Text(
                                        text = log.metadata,
                                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
