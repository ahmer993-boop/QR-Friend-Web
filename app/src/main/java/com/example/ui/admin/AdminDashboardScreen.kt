package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.common.AdminTab
import com.example.ui.theme.BorderColor
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenDark
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.DividerColor
import com.example.ui.theme.ErrorBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorText
import com.example.ui.theme.InfoBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoText
import com.example.ui.theme.PageBg
import com.example.ui.theme.SuccessBg
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessText
import com.example.ui.theme.TextHeadings
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningBg
import com.example.ui.theme.WarningText
import com.example.ui.theme.WarningYellow
import com.example.util.DateUtils
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>,
    users: List<UserEntity>,
    regions: List<RegionEntity>,
    tls: List<TlEntity>,
    transactions: List<TransactionEntity>,
    onNavigateTab: (AdminTab) -> Unit,
    onSelectVisit: (VisitEntity) -> Unit
) {
    var selectedRegionId by remember { mutableStateOf<Long?>(null) }
    var selectedTlId by remember { mutableStateOf<Long?>(null) }
    var selectedBdoId by remember { mutableStateOf<Long?>(null) }
    var selectedDateFilter by remember { mutableStateOf("All") } // "Today", "All"

    val todayDateString = DateUtils.getTodayDateString()

    // Filter merchants and visits based on selection
    val filteredMerchants = merchants.filter { m ->
        (selectedRegionId == null || m.regionId == selectedRegionId) &&
        (selectedTlId == null || m.tlId == selectedTlId) &&
        (selectedBdoId == null || m.bdoId == selectedBdoId)
    }

    val filteredVisits = visits.filter { v ->
        val m = merchants.find { it.merchantId == v.merchantId }
        val matchesRegion = selectedRegionId == null || (m != null && m.regionId == selectedRegionId)
        val matchesTl = selectedTlId == null || (m != null && m.tlId == selectedTlId)
        val matchesBdo = selectedBdoId == null || v.bdoId == selectedBdoId
        val matchesDate = selectedDateFilter == "All" || v.visitDateString == todayDateString

        matchesRegion && matchesTl && matchesBdo && matchesDate
    }

    val bdos = users.filter { it.role == "BDO" }
    val todayVisits = visits.filter { it.visitDateString == todayDateString }
    val newOnboardings = visits.filter { it.visitType.equals("New Onboarding", ignoreCase = true) }
    val qrDeployedVisits = visits.filter { it.qrDeployed }
    val qrActivatedVisits = visits.filter { it.visitType.equals("QR Activation", ignoreCase = true) || it.merchantResponse.equals("Activated", ignoreCase = true) }
    val activeMerchantsCount = filteredMerchants.count { it.merchantStatus.equals("Active", ignoreCase = true) }
    val gpsExceptionsCount = filteredVisits.count { it.gpsStatus == "GPS MISMATCH" || it.verificationStatus == "Needs Review" }

    val totalPaymentVolume = transactions.sumOf { it.amount }
    val totalTransactionsCount = transactions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("admin_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title & Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control Tower Dashboard",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextHeadings
                    )
                )
                Text(
                    text = "Real-time Field Activity, Verification & Acquisition Funnel",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Top Filter Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, BorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Region Filter
                    var regionExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = regionExpanded,
                        onExpandedChange = { regionExpanded = it },
                        modifier = Modifier.width(160.dp)
                    ) {
                        OutlinedTextField(
                            value = regions.find { it.id == selectedRegionId }?.name ?: "All Regions",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Region") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = BrandGreenDark,
                                unfocusedLabelColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = regionExpanded,
                            onDismissRequest = { regionExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Regions") },
                                onClick = {
                                    selectedRegionId = null
                                    regionExpanded = false
                                }
                            )
                            regions.forEach { reg ->
                                DropdownMenuItem(
                                    text = { Text(reg.name) },
                                    onClick = {
                                        selectedRegionId = reg.id
                                        regionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // TL Filter
                    var tlExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = tlExpanded,
                        onExpandedChange = { tlExpanded = it },
                        modifier = Modifier.width(160.dp)
                    ) {
                        OutlinedTextField(
                            value = tls.find { it.id == selectedTlId }?.name ?: "All TLs",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Team Leader") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tlExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = BrandGreenDark,
                                unfocusedLabelColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = tlExpanded,
                            onDismissRequest = { tlExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All TLs") },
                                onClick = {
                                    selectedTlId = null
                                    tlExpanded = false
                                }
                            )
                            tls.forEach { tl ->
                                DropdownMenuItem(
                                    text = { Text(tl.name) },
                                    onClick = {
                                        selectedTlId = tl.id
                                        tlExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // BDO Filter
                    var bdoExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = bdoExpanded,
                        onExpandedChange = { bdoExpanded = it },
                        modifier = Modifier.width(160.dp)
                    ) {
                        OutlinedTextField(
                            value = bdos.find { it.id == selectedBdoId }?.name ?: "All BDOs",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("BDO Agent") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bdoExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = BrandGreenDark,
                                unfocusedLabelColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = bdoExpanded,
                            onDismissRequest = { bdoExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All BDOs") },
                                onClick = {
                                    selectedBdoId = null
                                    bdoExpanded = false
                                }
                            )
                            bdos.forEach { bdo ->
                                DropdownMenuItem(
                                    text = { Text(bdo.name) },
                                    onClick = {
                                        selectedBdoId = bdo.id
                                        bdoExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Filter
                    var dateExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dateExpanded,
                        onExpandedChange = { dateExpanded = it },
                        modifier = Modifier.width(130.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedDateFilter,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = BrandGreenDark,
                                unfocusedLabelColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = dateExpanded,
                            onDismissRequest = { dateExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Dates") },
                                onClick = {
                                    selectedDateFilter = "All"
                                    dateExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Today") },
                                onClick = {
                                    selectedDateFilter = "Today"
                                    dateExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // KPI Cards Grid
        Text(
            text = "Key Performance Indicators",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Total BDOs",
                value = "${bdos.size}",
                subtitle = "Active in field",
                icon = Icons.Default.Group,
                modifier = Modifier.width(170.dp)
            )
            KpiCard(
                title = "Total Merchants",
                value = "${filteredMerchants.size}",
                subtitle = "$activeMerchantsCount active",
                icon = Icons.Default.Store,
                modifier = Modifier.width(170.dp)
            )
            KpiCard(
                title = "Today's Visits",
                value = "${todayVisits.size}",
                subtitle = "Logged today",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.width(170.dp)
            )
            KpiCard(
                title = "New Onboardings",
                value = "${newOnboardings.size}",
                subtitle = "Acquired",
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.width(170.dp)
            )
            KpiCard(
                title = "QR Deployed",
                value = "${qrDeployedVisits.size}",
                subtitle = "Active stands",
                icon = Icons.Default.QrCode,
                modifier = Modifier.width(170.dp)
            )
            KpiCard(
                title = "QR Activated",
                value = "${qrActivatedVisits.size}",
                subtitle = "Payment ready",
                icon = Icons.Default.Paid,
                modifier = Modifier.width(170.dp)
            )
            KpiCard(
                title = "GPS Exceptions",
                value = "$gpsExceptionsCount",
                subtitle = "Requires review",
                icon = Icons.Default.LocationOff,
                highlight = true,
                modifier = Modifier.width(170.dp)
            )
            KpiCard(
                title = "Payment Volume",
                value = "PKR ${"%,.0f".format(totalPaymentVolume)}",
                subtitle = "$totalTransactionsCount txns",
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.width(170.dp)
            )
        }

        // Alerts Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = WarningYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Exceptions & Alerts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextHeadings
                            )
                        )
                    }
                    Text(
                        text = "Take Action",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrandGreenDark, fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AlertItem(
                    severity = "high",
                    text = "$gpsExceptionsCount visits have GPS mismatch awaiting audit",
                    actionLabel = "Open Verification Queue",
                    onClick = { onNavigateTab(AdminTab.VERIFICATION) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AlertItem(
                    severity = "medium",
                    text = "${filteredMerchants.count { it.merchantStatus == "Inactive" }} merchants inactive for more than 7 days",
                    actionLabel = "View Inactive Merchants",
                    onClick = { onNavigateTab(AdminTab.MERCHANTS) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AlertItem(
                    severity = "info",
                    text = "${qrDeployedVisits.size} QR code deployments successfully verified today",
                    actionLabel = "View Visits",
                    onClick = { onNavigateTab(AdminTab.VISITS) }
                )
            }
        }

        // Full Business Funnel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "QR Merchant Acquisition & Monetization Funnel",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                )
                Text(
                    text = "From initial field visit to recurring QR transaction volume",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                FunnelStage(label = "Total Field Visits", count = filteredVisits.size, progress = 1.0f, color = BrandGreen)
                FunnelStage(label = "New Onboardings", count = newOnboardings.size, progress = 0.8f, color = BrandGreenDark)
                FunnelStage(label = "QR Deployed", count = qrDeployedVisits.size, progress = 0.65f, color = SuccessGreen)
                FunnelStage(label = "QR Activated", count = qrActivatedVisits.size, progress = 0.50f, color = BrandGreen)
                FunnelStage(label = "First Transactions", count = totalTransactionsCount, progress = 0.40f, color = WarningYellow)
                FunnelStage(label = "Recurring Payment Volume", count = totalTransactionsCount, progress = 0.30f, color = BrandGreenDark, customVal = "PKR ${"%,.0f".format(totalPaymentVolume)}")
            }
        }

        // Daily Activity Feed
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Activity Feed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                    )
                    Text(
                        text = "Newest First",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredVisits.isEmpty()) {
                    Text(
                        text = "No field visits recorded for current filter.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    filteredVisits.take(6).forEach { visit ->
                        val bdo = users.find { it.id == visit.bdoId }?.name ?: "BDO"
                        val merchant = merchants.find { it.merchantId == visit.merchantId }

                        ActivityFeedRow(
                            time = DateUtils.formatTime(visit.visitTime),
                            bdoName = bdo,
                            shopName = merchant?.shopName ?: visit.merchantId,
                            visitType = visit.visitType,
                            qrDeployed = visit.qrDeployed,
                            gpsStatus = visit.gpsStatus,
                            distanceMeters = visit.distanceMeters,
                            onClick = { onSelectVisit(visit) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = DividerColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color = BrandGreen,
    bgColor: Color = BrandGreenLight,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) ErrorBg else CardWhite
        ),
        border = BorderStroke(1.dp, if (highlight) Color(0xFFFCA5A5) else BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (highlight) Color(0xFFFFE4E6) else BrandGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (highlight) ErrorRed else BrandGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) ErrorText else TextHeadings,
                    fontSize = 22.sp
                )
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (highlight) ErrorRed else BrandGreenDark,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun AlertItem(
    severity: String,
    text: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    val (dotColor, bg, borderColor, textColor) = when (severity) {
        "high" -> Quadruple(ErrorRed, ErrorBg, Color(0xFFFCA5A5), ErrorText)
        "medium" -> Quadruple(WarningYellow, WarningBg, Color(0xFFFDE68A), WarningText)
        else -> Quadruple(InfoBlue, InfoBg, Color(0xFFBAE6FD), InfoText)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = dotColor
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = dotColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun FunnelStage(
    label: String,
    count: Int,
    progress: Float,
    color: Color,
    customVal: String? = null
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = TextPrimary)
            )
            Text(
                text = customVal ?: "$count",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = color)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = BrandGreenLight
        )
    }
}

@Composable
fun ActivityFeedRow(
    time: String,
    bdoName: String,
    shopName: String,
    visitType: String,
    qrDeployed: Boolean,
    gpsStatus: String,
    distanceMeters: Double,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bdoName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextHeadings
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "visited",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = shopName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreenDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = visitType,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
                Text(
                    text = if (qrDeployed) "QR Deployed" else "No QR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (qrDeployed) BrandGreenDark else TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // GPS status chip
        Surface(
            color = if (gpsStatus == "VALID") SuccessBg else ErrorBg,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, if (gpsStatus == "VALID") Color(0xFF86EFAC) else Color(0xFFFCA5A5))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (gpsStatus == "VALID") SuccessGreen else ErrorRed)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (gpsStatus == "VALID") "Valid (${GeoUtils.formatDistance(distanceMeters)})" else "Mismatch (${GeoUtils.formatDistance(distanceMeters)})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (gpsStatus == "VALID") SuccessText else ErrorText
                    )
                )
            }
        }
    }
}
