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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
fun VisitDetailDialog(
    visit: VisitEntity,
    merchant: MerchantEntity?,
    bdo: UserEntity?,
    onDismiss: () -> Unit,
    onUpdateVerification: (Long, String, String) -> Unit
) {
    var adminNotes by remember { mutableStateOf(visit.verificationNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("visit_detail_dialog")
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (visit.gpsStatus == "VALID") EmeraldSubtle else RedSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (visit.gpsStatus == "VALID") Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (visit.gpsStatus == "VALID") EmeraldDark else RedDanger
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Visit #${visit.id}: ${visit.visitType}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            )
                            Text(
                                text = "Date: ${DateUtils.formatDateTime(visit.visitTime)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GPS Comparison Banner
                Surface(
                    color = if (visit.gpsStatus == "VALID") EmeraldSubtle else RedSubtle,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GPS Status: ${visit.gpsStatus}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (visit.gpsStatus == "VALID") EmeraldDark else RedDanger
                                )
                            )
                            Text(
                                text = "Distance: ${GeoUtils.formatDistance(visit.distanceMeters)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (visit.gpsStatus == "VALID") EmeraldDark else RedDanger
                                )
                            )
                        }

                        Text(
                            text = "Registered GPS: ${GeoUtils.formatCoordinates(visit.merchantLatitude, visit.merchantLongitude)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Navy900)
                        )
                        Text(
                            text = "Actual Visit GPS: ${GeoUtils.formatCoordinates(visit.latitude, visit.longitude)} (Accuracy: ${visit.gpsAccuracy.toInt()}m)",
                            style = MaterialTheme.typography.labelSmall.copy(color = Navy900)
                        )

                        if (visit.antiFraudFlags.isNotBlank()) {
                            Text(
                                text = "🚨 Anti-Fraud Trigger: ${visit.antiFraudFlags}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RedDanger
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Photos Section
                Text(
                    text = "Photos Captured on Site",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Shop front photo card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Shopfront Photo",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                            )
                            Text(
                                text = if (visit.photoPath.isNotBlank()) "Captured & Verified" else "Simulated Site Photo",
                                style = MaterialTheme.typography.labelSmall.copy(color = EmeraldDark)
                            )
                        }
                    }

                    // QR standee photo card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "QR Standee Photo",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                            )
                            Text(
                                text = if (visit.qrDeployed) "QR Stand Verified" else "Not Deployed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (visit.qrDeployed) EmeraldDark else SlateMuted
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Questionnaire & Visit Answers
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Field Questionnaire Responses",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                        HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))

                        DetailRow(label = "Merchant Shop", value = merchant?.shopName ?: visit.merchantId)
                        DetailRow(label = "Field Agent (BDO)", value = bdo?.name ?: "BDO")
                        DetailRow(label = "Visit Type", value = visit.visitType)
                        DetailRow(label = "QR Deployed?", value = if (visit.qrDeployed) "Yes" else "No (${visit.qrNotDeployedReason})")
                        DetailRow(label = "Merchant Status", value = visit.merchantStatus)
                        DetailRow(label = "Payment Discussion", value = if (visit.paymentDiscussion) "Conducted" else "Not Conducted")
                        DetailRow(label = "Merchant Response", value = visit.merchantResponse)
                        DetailRow(label = "Follow-up Required", value = if (visit.followUpRequired) "Yes (${visit.followUpDate})" else "No")
                        DetailRow(label = "BDO Comments", value = if (visit.comments.isNotBlank()) visit.comments else "None")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Review Section
                Text(
                    text = "Verification Decision & Admin Audit",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = adminNotes,
                    onValueChange = { adminNotes = it },
                    label = { Text("Audit / Review Notes") },
                    placeholder = { Text("Enter reason for verification or rejection...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onUpdateVerification(visit.id, "Verified", adminNotes)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Verify Visit")
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateVerification(visit.id, "Needs Review", adminNotes)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Flag Review")
                    }

                    Button(
                        onClick = {
                            onUpdateVerification(visit.id, "Rejected", adminNotes)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reject")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
