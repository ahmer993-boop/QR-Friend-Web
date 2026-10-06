package com.example.ui.bdo

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.*

@Composable
fun BdoProfileScreen(
    bdoUser: UserEntity,
    region: RegionEntity?,
    tl: TlEntity?,
    isOfflineMode: Boolean,
    unsyncedCount: Int,
    onToggleOfflineMode: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(16.dp)
            .testTag("bdo_profile_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, BorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(BrandGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = bdoUser.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                        )
                        Text(
                            text = "@${bdoUser.username} • Business Development Officer",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = "Mobile: ${bdoUser.mobile}",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrandGreenDark, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Hierarchy info
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "Assigned Region", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                        Text(text = region?.name ?: "North Region", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings))
                    }
                    Column {
                        Text(text = "Reporting TL", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                        Text(text = tl?.name ?: "Ahmed Khan", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings))
                    }
                }
            }
        }

        // Offline Field Mode & Local Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, BorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isOfflineMode) Icons.Default.WifiOff else Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isOfflineMode) DangerText else BrandGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isOfflineMode) "Offline Mode (Simulated)" else "Online (Connected)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                            )
                            Text(
                                text = if (isOfflineMode) "Field visits will be cached locally" else "Visits sync directly to HQ server",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    Switch(
                        checked = isOfflineMode,
                        onCheckedChange = onToggleOfflineMode,
                        modifier = Modifier.testTag("offline_mode_toggle"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandGreen
                        )
                    )
                }

                HorizontalDivider(color = BorderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Unsynced Local Visits", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                        Text(
                            text = "$unsyncedCount visits waiting to sync",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (unsyncedCount > 0) DangerText else BrandGreenDark
                            )
                        )
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = !isOfflineMode && unsyncedCount > 0,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimaryBg,
                            contentColor = ButtonPrimaryText
                        ),
                        modifier = Modifier.testTag("sync_now_button")
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("bdo_logout_button"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DangerText.copy(alpha = 0.5f))
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = DangerText)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out from Agent Session", color = DangerText, fontWeight = FontWeight.Bold)
        }
    }
}
