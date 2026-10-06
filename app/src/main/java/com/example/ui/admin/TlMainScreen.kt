package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.MainViewModel
import com.example.ui.common.TlTab
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TlMainScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.tlTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val bdos by viewModel.tlBdos.collectAsState()
    val merchants by viewModel.tlMerchants.collectAsState()
    val visits by viewModel.tlVisits.collectAsState()
    val targets by viewModel.allTargets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val regions by viewModel.allRegions.collectAsState()
    val qrRequests by viewModel.tlQrRequests.collectAsState()
    val users by viewModel.allUsers.collectAsState()

    val tabs = listOf(
        Triple(TlTab.DASHBOARD, Icons.Default.Dashboard, "Team Dashboard"),
        Triple(TlTab.QR_OPERATIONS, Icons.Default.QrCodeScanner, "QR Operations"),
        Triple(TlTab.MY_BDOS, Icons.Default.Badge, "My BDOs"),
        Triple(TlTab.MERCHANTS, Icons.Default.Store, "Merchants"),
        Triple(TlTab.VISITS, Icons.Default.Visibility, "Team Visits"),
        Triple(TlTab.PERFORMANCE, Icons.Default.Assessment, "Target Status")
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "QR Friend",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextHeadings
                                    )
                                )
                                Text(
                                    text = "TL Hub: ${currentUser?.name ?: "Team Leader"} • Supervising ${bdos.size} BDOs",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("tl_logout_button")
                        ) {
                            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = TextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = White,
                        titleContentColor = TextHeadings,
                        actionIconContentColor = TextSecondary
                    )
                )
                // Top Navigation Task Bar
                Surface(
                    color = NavBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(tabs) { (tab, icon, label) ->
                            val isSelected = currentTab == tab
                            Surface(
                                color = if (isSelected) NavItemSelected else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { viewModel.setTlTab(tab) }
                                    .testTag("tl_tab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) NavIconSelected else NavIconNormal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) NavTextSelected else NavTextNormal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = DividerColor)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                TlTab.DASHBOARD -> TlDashboardView(
                    tlName = currentUser?.name ?: "Team Leader",
                    bdos = bdos,
                    merchants = merchants,
                    visits = visits
                )
                TlTab.QR_OPERATIONS -> QrOperationsScreen(
                    qrRequests = qrRequests,
                    merchants = merchants,
                    users = users,
                    currentUser = currentUser,
                    onCreateRequest = { viewModel.createQrRequest(it) },
                    onForwardToVendor = { req, date, ref, coord -> viewModel.forwardQrToVendor(req, date, ref, coord) },
                    onUpdateScanning = { req, status, disc -> viewModel.updateQrScanning(req, status, disc) },
                    onUpdateCourier = { req, courier, trk, dispDate, expDate -> viewModel.updateQrCourier(req, courier, trk, dispDate, expDate) },
                    onConfirmDelivery = { req, date -> viewModel.confirmQrDelivery(req, date) },
                    onEscalate = { req, comments -> viewModel.escalateQrRequest(req, comments) },
                    onSubmitJustification = { req, just -> viewModel.submitNonDeploymentJustification(req, just) }
                )
                TlTab.MY_BDOS -> TlBdosView(
                    bdos = bdos,
                    merchants = merchants,
                    visits = visits
                )
                TlTab.MERCHANTS -> MerchantListScreen(
                    merchants = merchants,
                    visits = visits,
                    users = bdos,
                    regions = regions,
                    tls = emptyList(),
                    transactions = transactions,
                    onSelectMerchant = { viewModel.viewMerchantDetail(it) },
                    onReassignMerchant = { mId, bdoId, tlId, rId ->
                        viewModel.reassignMerchant(mId, bdoId, tlId, rId)
                    },
                    onCreateMerchant = { viewModel.createMerchant(it) }
                )
                TlTab.VISITS -> AdminVisitsScreen(
                    visits = visits,
                    merchants = merchants,
                    users = bdos,
                    onSelectVisit = { viewModel.viewVisitDetail(it) }
                )
                TlTab.PERFORMANCE -> ReportsScreen(
                    merchants = merchants,
                    visits = visits,
                    users = bdos,
                    targets = targets,
                    transactions = transactions
                )
            }
        }
    }
}

@Composable
fun TlDashboardView(
    tlName: String,
    bdos: List<UserEntity>,
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>
) {
    val activeQrs = merchants.count { it.qrStatus == "Active" || it.qrStatus == "Deployed" }
    val completedVisits = visits.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Team Leader Command Hub",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Text(
                        text = "Supervising field visits, daily target tracking, and QR deployments for your BDO squad.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(label = "BDO Squad", value = bdos.size.toString(), color = Color(0xFFD97706), modifier = Modifier.weight(1f))
                        MetricBox(label = "Assigned Shops", value = merchants.size.toString(), color = Navy900, modifier = Modifier.weight(1f))
                        MetricBox(label = "Visits Done", value = completedVisits.toString(), color = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                        MetricBox(label = "QRs Deployed", value = activeQrs.toString(), color = EmeraldDark, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text(
                text = "Supervised BDO Squad (${bdos.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
            )
        }

        items(bdos) { bdo ->
            val bdoMerchants = merchants.filter { it.bdoId == bdo.id }
            val bdoVisits = visits.filter { it.bdoId == bdo.id }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = bdo.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                                )
                                Text(
                                    text = "Mobile: ${bdo.mobile} • Emp: ${bdo.employeeId.ifBlank { bdo.username }}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                                )
                            }
                        }

                        Surface(
                            color = if (bdo.status == "Active") EmeraldSubtle else RedSubtle,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = bdo.status,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (bdo.status == "Active") EmeraldDark else RedDanger
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Assigned Merchants: ${bdoMerchants.size}", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                        Text("Completed Visits: ${bdoVisits.size}", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldDark, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}

@Composable
fun TlBdosView(
    bdos: List<UserEntity>,
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(bdos) { bdo ->
            val bdoMerchants = merchants.filter { it.bdoId == bdo.id }
            val bdoVisits = visits.filter { it.bdoId == bdo.id }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Field Officer: ${bdo.name}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Text(
                        text = "Username: ${bdo.username} • Mobile: ${bdo.mobile}",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Portfolio: ${bdoMerchants.size} Merchants Assigned • ${bdoVisits.size} Total Visits Logged",
                        style = MaterialTheme.typography.labelSmall.copy(color = EmeraldDark, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}
