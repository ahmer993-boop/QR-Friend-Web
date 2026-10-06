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
import androidx.compose.material.icons.filled.SupervisorAccount
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
import com.example.ui.common.AsmTab
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsmMainScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.asmTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val tls by viewModel.asmTls.collectAsState()
    val bdos by viewModel.asmBdos.collectAsState()
    val merchants by viewModel.asmMerchants.collectAsState()
    val visits by viewModel.asmVisits.collectAsState()
    val regions by viewModel.allRegions.collectAsState()
    val targets by viewModel.allTargets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val qrRequests by viewModel.asmQrRequests.collectAsState()
    val users by viewModel.allUsers.collectAsState()

    val regionName = regions.find { it.id == currentUser?.regionId }?.name ?: "Assigned Region"

    val tabs = listOf(
        Triple(AsmTab.DASHBOARD, Icons.Default.Dashboard, "Area Dashboard"),
        Triple(AsmTab.QR_OPERATIONS, Icons.Default.QrCodeScanner, "QR Operations"),
        Triple(AsmTab.TLS, Icons.Default.SupervisorAccount, "Assigned TLs"),
        Triple(AsmTab.BDOS, Icons.Default.Badge, "Area BDOs"),
        Triple(AsmTab.MERCHANTS, Icons.Default.Store, "Merchants"),
        Triple(AsmTab.VISITS, Icons.Default.Visibility, "Visits Stream"),
        Triple(AsmTab.PERFORMANCE, Icons.Default.Assessment, "Target Performance")
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
                                    imageVector = Icons.Default.SupervisorAccount,
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
                                    text = "ASM: ${currentUser?.name ?: "ASM"} • Territory: $regionName",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("asm_logout_button")
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
                                    .clickable { viewModel.setAsmTab(tab) }
                                    .testTag("asm_tab_${tab.name.lowercase()}")
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
                AsmTab.DASHBOARD -> AsmDashboardView(
                    regionName = regionName,
                    tls = tls,
                    bdos = bdos,
                    merchants = merchants,
                    visits = visits
                )
                AsmTab.QR_OPERATIONS -> QrOperationsScreen(
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
                AsmTab.TLS -> AsmTlsView(
                    tls = tls,
                    bdos = bdos,
                    merchants = merchants,
                    visits = visits
                )
                AsmTab.BDOS -> AsmBdosView(
                    bdos = bdos,
                    merchants = merchants,
                    visits = visits
                )
                AsmTab.MERCHANTS -> MerchantListScreen(
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
                AsmTab.VISITS -> AdminVisitsScreen(
                    visits = visits,
                    merchants = merchants,
                    users = bdos,
                    onSelectVisit = { viewModel.viewVisitDetail(it) }
                )
                AsmTab.PERFORMANCE -> ReportsScreen(
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
fun AsmDashboardView(
    regionName: String,
    tls: List<UserEntity>,
    bdos: List<UserEntity>,
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>
) {
    val activeQrs = merchants.count { it.qrStatus == "Active" || it.qrStatus == "Deployed" }
    val todayVisits = visits.size
    val gpsVerified = visits.count { it.gpsStatus == "GPS MATCH" || it.verificationStatus == "Verified" }

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
                        text = "Territory Overview: $regionName",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    )
                    Text(
                        text = "Real-time hierarchy tracking and performance metrics for your assigned area.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricBox(label = "Team Leaders", value = tls.size.toString(), color = Color(0xFFD97706), modifier = Modifier.weight(1f))
                        MetricBox(label = "Field Officers", value = bdos.size.toString(), color = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                        MetricBox(label = "Merchants", value = merchants.size.toString(), color = Navy900, modifier = Modifier.weight(1f))
                        MetricBox(label = "QRs Deployed", value = activeQrs.toString(), color = EmeraldDark, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text(
                text = "Assigned Team Leaders (${tls.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
            )
        }

        items(tls) { tl ->
            val teamBdos = bdos.filter { it.tlId == tl.id }
            val teamMerchants = merchants.filter { it.tlId == tl.id }
            val teamVisits = visits.filter { it.tlId == tl.id }

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
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = tl.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                                )
                                Text(
                                    text = "Username: ${tl.username} • Mobile: ${tl.mobile}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${teamBdos.size} BDOs",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
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
                        Text("Merchants in Territory: ${teamMerchants.size}", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                        Text("Completed Visits: ${teamVisits.size}", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldDark, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}

@Composable
fun AsmTlsView(
    tls: List<UserEntity>,
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
        items(tls) { tl ->
            val assignedBdos = bdos.filter { it.tlId == tl.id }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TL: ${tl.name} (${tl.employeeId.ifBlank { tl.username }})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Text(
                        text = "Mobile: ${tl.mobile} • Email: ${tl.email}",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Supervised Field Officers (${assignedBdos.size}):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Navy900)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        assignedBdos.forEach { bdo ->
                            Surface(
                                color = EmeraldSubtle,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = bdo.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = EmeraldDark, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AsmBdosView(
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BDO: ${bdo.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                            )
                            Text(
                                text = "Username: ${bdo.username} • Mobile: ${bdo.mobile}",
                                style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                            )
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Assigned Portfolio: ${bdoMerchants.size} merchants • Logged Visits: ${bdoVisits.size}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF475569))
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = color)
            )
        }
    }
}
