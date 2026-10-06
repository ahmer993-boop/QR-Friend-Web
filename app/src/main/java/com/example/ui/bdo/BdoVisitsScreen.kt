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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.*
import com.example.util.DateUtils
import com.example.util.GeoUtils

@Composable
fun BdoVisitsScreen(
    visits: List<VisitEntity>,
    merchants: List<MerchantEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(16.dp)
            .testTag("bdo_visits_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Field Visit History",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextHeadings
                    )
                )
                Text(
                    text = "${visits.size} logged visits • Locked & verified records",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        if (visits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "You have not submitted any visits yet today.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visits, key = { it.id }) { visit ->
                    val merchant = merchants.find { it.merchantId == visit.merchantId }
                    BdoVisitHistoryCard(visit = visit, merchant = merchant)
                }
            }
        }
    }
}

@Composable
fun BdoVisitHistoryCard(
    visit: VisitEntity,
    merchant: MerchantEntity?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bdo_visit_card_${visit.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = merchant?.shopName ?: visit.merchantId,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextHeadings
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "${visit.visitType} • ${DateUtils.formatDateTime(visit.visitTime)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // GPS status badge
                    val isGpsValid = visit.gpsStatus == "VALID"
                    Surface(
                        color = if (isGpsValid) SuccessBg else DangerBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isGpsValid) Color(0xFF86EFAC) else Color(0xFFFCA5A5))
                    ) {
                        Text(
                            text = "${visit.gpsStatus} (${GeoUtils.formatDistance(visit.distanceMeters)})",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isGpsValid) SuccessText else DangerText
                            )
                        )
                    }

                    // Verification status badge
                    val (vBg, vText, vBorder) = when (visit.verificationStatus) {
                        "Verified" -> Triple(SuccessBg, SuccessText, Color(0xFF86EFAC))
                        "Rejected" -> Triple(DangerBg, DangerText, Color(0xFFFCA5A5))
                        "Needs Review" -> Triple(WarningBg, WarningOrange, Color(0xFFFDE68A))
                        else -> Triple(InfoBg, InfoText, Color(0xFFBAE6FD))
                    }
                    Surface(
                        color = vBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, vBorder)
                    ) {
                        Text(
                            text = visit.verificationStatus,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = vText
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = if (visit.qrDeployed) BrandGreenDark else TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (visit.qrDeployed) "QR Standee Active" else "No QR (${visit.qrNotDeployedReason})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (visit.qrDeployed) BrandGreenDark else TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (visit.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (visit.isSynced) BrandGreenDark else WarningOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (visit.isSynced) "Synced to HQ" else "Cached Locally",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (visit.isSynced) BrandGreenDark else WarningOrange
                        )
                    )
                }
            }

            if (visit.comments.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${visit.comments}\"",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                    maxLines = 2
                )
            }
        }
    }
}
