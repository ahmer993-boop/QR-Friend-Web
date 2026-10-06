package com.example.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.TransactionEntity
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
fun MerchantDetailDialog(
    merchant: MerchantEntity,
    visits: List<VisitEntity>,
    transactions: List<TransactionEntity>,
    users: List<UserEntity>,
    regions: List<RegionEntity>,
    tls: List<TlEntity>,
    onDismiss: () -> Unit,
    onViewVisit: (VisitEntity) -> Unit
) {
    val bdo = users.find { it.id == merchant.bdoId }
    val tl = tls.find { it.id == merchant.tlId }
    val region = regions.find { it.id == merchant.regionId }

    val merchantVisits = visits.filter { it.merchantId == merchant.merchantId }.sortedByDescending { it.visitTime }
    val merchantTxns = transactions.filter { it.merchantId == merchant.merchantId }.sortedByDescending { it.transactionDate }

    val totalVolume = merchantTxns.sumOf { it.amount }
    val totalCount = merchantTxns.size

    // Determine health score
    val health = when {
        merchantTxns.isNotEmpty() -> "ACTIVE (Regular Payments)"
        merchantVisits.isNotEmpty() -> "NEEDS ATTENTION (Visited, No Transactions)"
        else -> "INACTIVE (Pending Field Visit)"
    }
    val healthColor = when {
        merchantTxns.isNotEmpty() -> EmeraldDark
        merchantVisits.isNotEmpty() -> AmberWarning
        else -> RedDanger
    }
    val healthBg = when {
        merchantTxns.isNotEmpty() -> EmeraldSubtle
        merchantVisits.isNotEmpty() -> AmberSubtle
        else -> RedSubtle
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("merchant_detail_dialog")
            ) {
                // Header with close button
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
                                .background(PrimaryBlueSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = PrimaryBlue)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = merchant.shopName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            )
                            Text(
                                text = "Merchant ID: ${merchant.merchantId}",
                                style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Health Indicator Banner
                Surface(
                    color = healthBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(healthColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Merchant Health: $health",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = healthColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Account & Contact Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Merchant Information",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                        HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))

                        DetailRow(label = "Owner Name", value = merchant.merchantName)
                        DetailRow(label = "Category", value = merchant.merchantCategory)
                        DetailRow(label = "Phone", value = merchant.mobile)
                        DetailRow(label = "Address", value = merchant.address)
                        DetailRow(label = "City", value = merchant.city)
                        DetailRow(label = "Registered Coordinates", value = GeoUtils.formatCoordinates(merchant.latitude, merchant.longitude))
                        DetailRow(label = "Region", value = region?.name ?: "N/A")
                        DetailRow(label = "Assigned TL", value = tl?.name ?: "N/A")
                        DetailRow(label = "Assigned BDO", value = bdo?.name ?: "Unassigned")
                        DetailRow(label = "QR ID", value = if (merchant.qrId.isNotBlank()) merchant.qrId else "Not Deployed")
                        DetailRow(label = "QR Status", value = merchant.qrStatus)
                        DetailRow(label = "Merchant Status", value = merchant.merchantStatus)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transaction Performance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSubtle),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "QR Transaction Performance",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Total Volume", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                                Text(
                                    text = "PKR ${"%,.0f".format(totalVolume)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                                )
                            }
                            Column {
                                Text(text = "Transactions", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                                Text(
                                    text = "$totalCount payments",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                                )
                            }
                            Column {
                                Text(text = "Frequency", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                                Text(
                                    text = if (totalCount > 0) "${totalCount}x / month" else "0x",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                                )
                            }
                        }

                        if (merchantTxns.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = EmeraldDark.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Recent Transactions:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                            )
                            merchantTxns.take(3).forEach { txn ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "${txn.transactionId} • ${DateUtils.formatDateTime(txn.transactionDate)}", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                                    Text(text = "PKR ${"%,.0f".format(txn.amount)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Field Visit History Card
                Text(
                    text = "Field Visit History (${merchantVisits.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (merchantVisits.isEmpty()) {
                    Text(
                        text = "No field visits recorded yet for this merchant.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                    )
                } else {
                    merchantVisits.forEach { v ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = v.visitType,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                                    )
                                    Surface(
                                        color = if (v.gpsStatus == "VALID") EmeraldSubtle else RedSubtle,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${v.gpsStatus} (${GeoUtils.formatDistance(v.distanceMeters)})",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (v.gpsStatus == "VALID") EmeraldDark else RedDanger
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "${DateUtils.formatDateTime(v.visitTime)} • By ${users.find { it.id == v.bdoId }?.name ?: "BDO"}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                                )

                                if (v.comments.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${v.comments}\"",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Navy900)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = { onViewVisit(v) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("View Visit Details & Photo", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Navy900)
        )
    }
}
