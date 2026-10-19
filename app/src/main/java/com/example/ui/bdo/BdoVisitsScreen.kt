package com.example.ui.bdo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

enum class VisitFilterTab {
    CONDUCTED,
    PENDING
}

@Composable
fun BdoVisitsScreen(
    visits: List<VisitEntity>,
    merchants: List<MerchantEntity>,
    onStartVisit: ((MerchantEntity) -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(VisitFilterTab.CONDUCTED) }
    val today = DateUtils.getTodayDateString()

    // Tab A: "Today's Conducted Visits"
    val conductedVisitsToday = remember(visits, today) {
        visits.filter { it.visitDateString == today }
    }

    // Tab B: "Pending / Required Visits"
    // Merchants assigned to BDO that have NOT yet been visited today
    val visitedMerchantIdsToday = remember(conductedVisitsToday) {
        conductedVisitsToday.map { it.merchantId }.toSet()
    }
    val pendingMerchants = remember(merchants, visitedMerchantIdsToday) {
        merchants.filter { !visitedMerchantIdsToday.contains(it.merchantId) }
    }

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
                    text = "Field Visits & Tasks",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextHeadings
                    )
                )
                Text(
                    text = "${conductedVisitsToday.size} completed today • ${pendingMerchants.size} pending remaining",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Distinct Requirement 4 Tabs: Tab A (Today's Conducted Visits) vs Tab B (Pending / Required Visits)
        TabRow(
            selectedTabIndex = if (selectedTab == VisitFilterTab.CONDUCTED) 0 else 1,
            containerColor = Color.White,
            contentColor = BrandGreen,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == VisitFilterTab.CONDUCTED,
                onClick = { selectedTab = VisitFilterTab.CONDUCTED },
                modifier = Modifier.testTag("tab_conducted_visits"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == VisitFilterTab.CONDUCTED) BrandGreen else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Today's Conducted (${conductedVisitsToday.size})",
                            fontWeight = if (selectedTab == VisitFilterTab.CONDUCTED) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == VisitFilterTab.PENDING,
                onClick = { selectedTab = VisitFilterTab.PENDING },
                modifier = Modifier.testTag("tab_pending_visits"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PendingActions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == VisitFilterTab.PENDING) WarningOrange else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pending / Required (${pendingMerchants.size})",
                            fontWeight = if (selectedTab == VisitFilterTab.PENDING) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        // Content Display based on active Tab
        when (selectedTab) {
            VisitFilterTab.CONDUCTED -> {
                if (conductedVisitsToday.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No visits completed yet today.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Switch to 'Pending' tab to choose and log assigned merchant visits.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(conductedVisitsToday, key = { it.id }) { visit ->
                            val merchant = merchants.find { it.merchantId == visit.merchantId }
                            BdoVisitHistoryCard(visit = visit, merchant = merchant)
                        }
                    }
                }
            }
            VisitFilterTab.PENDING -> {
                if (pendingMerchants.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "All required merchant visits completed today!",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextHeadings, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Great job! You have fulfilled today's visit roster.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pendingMerchants, key = { it.merchantId }) { merchant ->
                            BdoPendingMerchantCard(merchant = merchant, onStartVisit = onStartVisit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BdoPendingMerchantCard(
    merchant: MerchantEntity,
    onStartVisit: ((MerchantEntity) -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_merchant_card_${merchant.merchantId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = merchant.shopName.ifBlank { merchant.businessName },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextHeadings
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${merchant.merchantId} • ${merchant.merchantCategory}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = merchant.address.ifBlank { "Assigned Route Location" },
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                    maxLines = 1
                )
            }
            if (onStartVisit != null) {
                Button(
                    onClick = { onStartVisit(merchant) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Visit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    color = WarningBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Text(
                        text = "Pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                    )
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
                            text = merchant?.shopName ?: visit.merchantName.ifBlank { visit.merchantId },
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
