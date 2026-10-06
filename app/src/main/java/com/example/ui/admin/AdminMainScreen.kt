package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.common.AdminTab
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: MainViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentTab by viewModel.adminTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val merchants by viewModel.allMerchants.collectAsState()
    val visits by viewModel.allVisits.collectAsState()
    val verificationQueue by viewModel.verificationQueue.collectAsState()
    val regions by viewModel.allRegions.collectAsState()
    val tls by viewModel.allTls.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val targets by viewModel.allTargets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val gpsThreshold by viewModel.gpsThreshold.collectAsState()
    val qrRequests by viewModel.allQrRequests.collectAsState()
    val cloudStatus by viewModel.cloudStatus.collectAsState()

    val selectedMerchantForDetail by viewModel.selectedMerchantForDetail.collectAsState()
    val selectedVisitForDetail by viewModel.selectedVisitForDetail.collectAsState()

    val tabs = listOf(
        Triple(AdminTab.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
        Triple(AdminTab.QR_OPERATIONS, Icons.Default.QrCodeScanner, "QR Operations"),
        Triple(AdminTab.MERCHANTS, Icons.Default.Store, "Merchants"),
        Triple(AdminTab.FIELD_TEAM, Icons.Default.Group, "Field Team"),
        Triple(AdminTab.VISITS, Icons.Default.Visibility, "Visits"),
        Triple(AdminTab.MAP_VIEW, Icons.Default.Map, "Map View"),
        Triple(AdminTab.VERIFICATION, Icons.Default.Security, "Verification"),
        Triple(AdminTab.TARGETS, Icons.Default.TrackChanges, "Targets"),
        Triple(AdminTab.REPORTS, Icons.Default.Assessment, "Reports"),
        Triple(AdminTab.EXCEL_UPLOAD, Icons.Default.FileUpload, "Excel Import"),
        Triple(AdminTab.SETTINGS, Icons.Default.Settings, "Settings")
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
                                    imageVector = Icons.Default.QrCodeScanner,
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
                                    text = "Admin: ${currentUser?.name ?: "Admin"} • Full Access",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("admin_logout_button")
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
                            val isVerificationTab = tab == AdminTab.VERIFICATION
                            val queueCount = verificationQueue.size

                            Surface(
                                color = if (isSelected) NavItemSelected else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { viewModel.setAdminTab(tab) }
                                    .testTag("tab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isVerificationTab && queueCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = RedDanger) {
                                                    Text("$queueCount")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) NavIconSelected else NavIconNormal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) NavIconSelected else NavIconNormal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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
                AdminTab.DASHBOARD -> AdminDashboardScreen(
                    merchants = merchants,
                    visits = visits,
                    users = users,
                    regions = regions,
                    tls = tls,
                    transactions = transactions,
                    onNavigateTab = { viewModel.setAdminTab(it) },
                    onSelectVisit = { viewModel.viewVisitDetail(it) }
                )
                AdminTab.QR_OPERATIONS -> QrOperationsScreen(
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
                AdminTab.MERCHANTS -> MerchantListScreen(
                    merchants = merchants,
                    visits = visits,
                    users = users,
                    regions = regions,
                    tls = tls,
                    transactions = transactions,
                    onSelectMerchant = { viewModel.viewMerchantDetail(it) },
                    onReassignMerchant = { mId, bdoId, tlId, rId ->
                        viewModel.reassignMerchant(mId, bdoId, tlId, rId)
                    },
                    onCreateMerchant = { viewModel.createMerchant(it) }
                )
                AdminTab.FIELD_TEAM -> FieldTeamScreen(
                    users = users,
                    regions = regions,
                    tls = tls,
                    merchants = merchants,
                    visits = visits,
                    targets = targets,
                    onCreateRegion = { viewModel.createRegion(it) },
                    onCreateTl = { name, rId -> viewModel.createTl(name, rId) },
                    onCreateBdo = { uName, name, mob, rId, tlId -> viewModel.createBdo(uName, name, mob, rId, tlId) }
                )
                AdminTab.VISITS -> AdminVisitsScreen(
                    visits = visits,
                    merchants = merchants,
                    users = users,
                    onSelectVisit = { viewModel.viewVisitDetail(it) }
                )
                AdminTab.MAP_VIEW -> MapViewScreen(
                    visits = visits,
                    merchants = merchants,
                    users = users,
                    onSelectVisit = { viewModel.viewVisitDetail(it) }
                )
                AdminTab.VERIFICATION -> VerificationQueueScreen(
                    verificationQueue = verificationQueue,
                    merchants = merchants,
                    users = users,
                    onSelectVisit = { viewModel.viewVisitDetail(it) },
                    onUpdateVerification = { vId, status, notes ->
                        viewModel.updateVisitVerification(vId, status, notes)
                    }
                )
                AdminTab.TARGETS -> TargetsScreen(
                    users = users,
                    targets = targets,
                    visits = visits,
                    onUpdateTarget = { viewModel.upsertTarget(it) }
                )
                AdminTab.REPORTS -> ReportsScreen(
                    merchants = merchants,
                    visits = visits,
                    users = users,
                    targets = targets,
                    transactions = transactions
                )
                AdminTab.EXCEL_UPLOAD -> ExcelUploadScreen(
                    existingMerchants = merchants,
                    users = users,
                    regions = regions,
                    tls = tls,
                    onImportMerchants = { list, onDone ->
                        viewModel.importMerchants(list, onDone)
                    }
                )
                AdminTab.SETTINGS -> SettingsScreen(
                    currentGpsThreshold = gpsThreshold,
                    auditLogs = auditLogs,
                    onUpdateGpsThreshold = { viewModel.setGpsThreshold(it) },
                    cloudStatus = cloudStatus,
                    onTriggerSync = { viewModel.triggerCloudSync() }
                )
            }

            // Merchant Detail Dialog
            if (selectedMerchantForDetail != null) {
                MerchantDetailDialog(
                    merchant = selectedMerchantForDetail!!,
                    visits = visits,
                    transactions = transactions,
                    users = users,
                    regions = regions,
                    tls = tls,
                    onDismiss = { viewModel.viewMerchantDetail(null) },
                    onViewVisit = { v ->
                        viewModel.viewMerchantDetail(null)
                        viewModel.viewVisitDetail(v)
                    }
                )
            }

            // Visit Detail Dialog
            if (selectedVisitForDetail != null) {
                val v = selectedVisitForDetail!!
                val m = merchants.find { it.merchantId == v.merchantId }
                val bdo = users.find { it.id == v.bdoId }

                VisitDetailDialog(
                    visit = v,
                    merchant = m,
                    bdo = bdo,
                    onDismiss = { viewModel.viewVisitDetail(null) },
                    onUpdateVerification = { vId, status, notes ->
                        viewModel.updateVisitVerification(vId, status, notes)
                    }
                )
            }
        }
    }
}
