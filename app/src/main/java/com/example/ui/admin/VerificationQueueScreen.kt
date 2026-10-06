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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.AmberSubtle
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RedSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.DateUtils
import com.example.util.GeoUtils

@Composable
fun VerificationQueueScreen(
    verificationQueue: List<VisitEntity>,
    merchants: List<MerchantEntity>,
    users: List<UserEntity>,
    onSelectVisit: (VisitEntity) -> Unit,
    onUpdateVerification: (Long, String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("verification_queue_screen"),
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
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = RedDanger)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Anti-Fraud Verification Queue",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    )
                }
                Text(
                    text = "Automated GPS discrepancy detection & manual audit approval workflow",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }

            Surface(
                color = if (verificationQueue.isNotEmpty()) RedSubtle else EmeraldSubtle,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${verificationQueue.size} Flagged",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (verificationQueue.isNotEmpty()) RedDanger else EmeraldDark
                    )
                )
            }
        }

        if (verificationQueue.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EmeraldSubtle),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Verification Queue Clean",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                        )
                        Text(
                            text = "All submitted field visits have passed GPS radius verification and anti-fraud criteria.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Navy900)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(verificationQueue, key = { it.id }) { visit ->
                    val merchant = merchants.find { it.merchantId == visit.merchantId }
                    val bdo = users.find { it.id == visit.bdoId }

                    QueueItemCard(
                        visit = visit,
                        merchant = merchant,
                        bdo = bdo,
                        onViewDetails = { onSelectVisit(visit) },
                        onApprove = { notes -> onUpdateVerification(visit.id, "Verified", notes) },
                        onReject = { notes -> onUpdateVerification(visit.id, "Rejected", notes) },
                        onRequestRevisit = { notes -> onUpdateVerification(visit.id, "Needs Review", notes) }
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemCard(
    visit: VisitEntity,
    merchant: MerchantEntity?,
    bdo: UserEntity?,
    onViewDetails: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onRequestRevisit: (String) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var showActionInputs by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("queue_item_${visit.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RedSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.LocationOff, contentDescription = null, tint = RedDanger, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = merchant?.shopName ?: visit.merchantId,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                        Text(
                            text = "BDO: ${bdo?.name ?: "BDO"} • ${DateUtils.formatDateTime(visit.visitTime)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                        )
                    }
                }

                Surface(
                    color = RedSubtle,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "MISMATCH: ${GeoUtils.formatDistance(visit.distanceMeters)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RedDanger)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Discrepancy details
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Discrepancy Details:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Text(
                        text = "• Registered Coordinates: ${GeoUtils.formatCoordinates(visit.merchantLatitude, visit.merchantLongitude)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                    )
                    Text(
                        text = "• BDO Actual GPS: ${GeoUtils.formatCoordinates(visit.latitude, visit.longitude)} (±${visit.gpsAccuracy.toInt()}m)",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                    )
                    if (visit.antiFraudFlags.isNotBlank()) {
                        Text(
                            text = "• Flag: ${visit.antiFraudFlags}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RedDanger)
                        )
                    }
                    if (visit.comments.isNotBlank()) {
                        Text(
                            text = "• Agent Comment: \"${visit.comments}\"",
                            style = MaterialTheme.typography.labelSmall.copy(color = Navy900)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!showActionInputs) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Photos & Full Audit", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { showActionInputs = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Review Decision", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                // Expanded Action Inputs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Audit / Decision Notes") },
                        placeholder = { Text("Specify why approved, rejected, or flagged for revisit...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onApprove(notes)
                                showActionInputs = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Approve Visit", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                onRequestRevisit(notes)
                                showActionInputs = false
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Request Re-visit", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                onReject(notes)
                                showActionInputs = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reject", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
