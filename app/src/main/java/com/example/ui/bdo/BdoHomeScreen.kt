package com.example.ui.bdo

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import com.example.data.local.entity.TargetEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.admin.KpiCard
import com.example.ui.admin.QrBadge
import com.example.ui.admin.StatusBadge
import com.example.ui.theme.*
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BdoHomeScreen(
    bdoUser: UserEntity,
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>,
    target: TargetEntity?,
    onStartVisit: (MerchantEntity) -> Unit,
    onViewMerchant: (MerchantEntity) -> Unit,
    onViewAllMerchants: () -> Unit
) {
    val today = DateUtils.getTodayDateString()
    val todayVisits = visits.filter { it.bdoId == bdoUser.id && it.visitDateString == today }

    val actualVisits = todayVisits.size
    val targetVisits = target?.visitTarget ?: 10

    val actualOnboardings = todayVisits.count { it.visitType.equals("New Onboarding", true) }
    val targetOnboardings = target?.onboardingTarget ?: 5

    val actualQr = todayVisits.count { it.qrDeployed }
    val targetQr = target?.qrTarget ?: 5

    val actualActivation = todayVisits.count { it.visitType.equals("QR Activation", true) || it.merchantResponse.equals("Activated", true) }
    val targetActivation = target?.activationTarget ?: 4

    val unvisitedToday = merchants.filter { m ->
        todayVisits.none { it.merchantId == m.merchantId }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("bdo_home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Brand Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BrandGreenDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "QR Friend",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BrandGreenDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = BrandGreenLight,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = "BDO",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = BrandGreenDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Welcome, ${bdoUser.name}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextHeadings
                                    )
                                )
                                Text(
                                    text = "Field Force Agent • ${DateUtils.formatDateString(today)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }

                        Surface(
                            color = BrandGreenLight,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$actualVisits / $targetVisits Done",
                                    style = MaterialTheme.typography.labelSmall.copy(color = BrandGreenDark, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Two Large Action Cards (My Merchants & Start Visit)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: My Merchants
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewAllMerchants() }
                        .testTag("action_my_merchants"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, BorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "My Merchants",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextHeadings
                            )
                        )
                        Text(
                            text = "View assigned merchants & track status",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Card 2: Start Visit
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val next = unvisitedToday.firstOrNull() ?: merchants.firstOrNull()
                            if (next != null) onStartVisit(next) else onViewAllMerchants()
                        }
                        .testTag("action_start_visit"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, BorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = BrandGreenDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Start Visit",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextHeadings
                            )
                        )
                        Text(
                            text = "Capture location & deploy verified QR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Secondary Menu Action Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Item 1: Visit History
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAllMerchants() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Visit History", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = DividerColor)

                    // Item 2: QR Deployment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAllMerchants() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("QR Deployment & Standees", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = DividerColor)

                    // Item 3: My Performance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("My Performance & KPIs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = DividerColor)

                    // Item 4: Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Settings & Offline Storage", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                }
            }
        }

        // Today's Performance Summary Cards
        item {
            Text(
                text = "Today's Target Progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BdoStatCard(
                    title = "Visits",
                    value = "$actualVisits / $targetVisits",
                    color = BrandGreen,
                    bgColor = BrandGreenLight,
                    icon = Icons.Default.DirectionsWalk,
                    modifier = Modifier.weight(1f)
                )
                BdoStatCard(
                    title = "Onboarding",
                    value = "$actualOnboardings / $targetOnboardings",
                    color = BrandGreenDark,
                    bgColor = BrandGreenLight,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                BdoStatCard(
                    title = "QR Deployed",
                    value = "$actualQr / $targetQr",
                    color = SuccessGreen,
                    bgColor = SuccessBg,
                    icon = Icons.Default.QrCode,
                    modifier = Modifier.weight(1f)
                )
                BdoStatCard(
                    title = "QR Activated",
                    value = "$actualActivation / $targetActivation",
                    color = BrandGreenDark,
                    bgColor = BrandGreenLight,
                    icon = Icons.Default.Paid,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Priority Merchant Queue (Unvisited today)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assigned Merchants to Visit (${unvisitedToday.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                )
                OutlinedButton(
                    onClick = onViewAllMerchants,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BrandGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonSecondaryText)
                ) {
                    Text("View All (${merchants.size})", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (unvisitedToday.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessBg),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "All Assigned Merchants Visited Today!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SuccessText)
                            )
                            Text(
                                text = "Great job completing your planned visits. You can revisit or onboard new merchants.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            }
        } else {
            items(unvisitedToday.take(6), key = { it.id }) { merchant ->
                BdoMerchantActionCard(
                    merchant = merchant,
                    onVisit = { onStartVisit(merchant) },
                    onView = { onViewMerchant(merchant) }
                )
            }
        }
    }
}

@Composable
fun BdoStatCard(
    title: String,
    value: String,
    color: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings))
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
        }
    }
}

@Composable
fun BdoMerchantActionCard(
    merchant: MerchantEntity,
    onVisit: () -> Unit,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bdo_merchant_item_${merchant.merchantId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = merchant.shopName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextHeadings
                        )
                    )
                    Text(
                        text = "ID: ${merchant.merchantId} • Contact: ${merchant.merchantName}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(status = merchant.merchantStatus)
                    QrBadge(status = merchant.qrStatus)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${merchant.address}, ${merchant.city}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onView,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ButtonSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonSecondaryText)
                ) {
                    Text("Details", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onVisit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimaryBg,
                        contentColor = ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("start_visit_button_${merchant.merchantId}")
                ) {
                    Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Field Visit", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
