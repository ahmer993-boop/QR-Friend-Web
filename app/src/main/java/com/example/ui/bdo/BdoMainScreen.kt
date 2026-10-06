package com.example.ui.bdo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.admin.MerchantDetailDialog
import com.example.ui.admin.QrOperationsScreen
import com.example.ui.common.BdoTab
import com.example.ui.theme.*
import com.example.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BdoMainScreen(
    viewModel: MainViewModel
) {
    val currentTab by viewModel.bdoTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val assignedMerchants by viewModel.bdoMerchants.collectAsState()
    val bdoVisits by viewModel.bdoVisits.collectAsState()
    val todayTarget by viewModel.bdoTodayTarget.collectAsState()
    val regions by viewModel.allRegions.collectAsState()
    val tls by viewModel.allTls.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val unsyncedCount by viewModel.unsyncedVisitsCount.collectAsState()
    val gpsThreshold by viewModel.gpsThreshold.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val qrRequests by viewModel.allQrRequests.collectAsState()
    val users by viewModel.allUsers.collectAsState()

    val activeMerchantForVisit by viewModel.activeMerchantForVisit.collectAsState()
    val selectedMerchantForDetail by viewModel.selectedMerchantForDetail.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val user = currentUser ?: return
    val region = regions.find { it.id == user.regionId }
    val tl = tls.find { it.id == user.tlId }

    val tabs = listOf(
        Triple(BdoTab.HOME, Icons.Default.Home, "Home"),
        Triple(BdoTab.MERCHANTS, Icons.Default.Store, "Shops"),
        Triple(BdoTab.VISITS, Icons.Default.Visibility, "Visits"),
        Triple(BdoTab.QR_REQUESTS, Icons.Default.QrCodeScanner, "QR Requests"),
        Triple(BdoTab.TARGETS, Icons.Default.TrackChanges, "Targets"),
        Triple(BdoTab.PROFILE, Icons.Default.Person, "Profile")
    )

    // If active merchant for visit is selected, show full visit recording flow
    if (activeMerchantForVisit != null) {
        BdoVisitScreen(
            merchant = activeMerchantForVisit!!,
            bdoUser = user,
            gpsThreshold = gpsThreshold,
            onBack = { viewModel.startVisitForMerchant(null) },
            onSubmitVisit = { merchantId, visitType, lat, lon, acc, photo, qrDep, qrReason, status, paymentDisc, resp, comments, followUp, followUpDate ->
                viewModel.submitFieldVisit(
                    merchantId = merchantId,
                    visitType = visitType,
                    lat = lat,
                    lon = lon,
                    accuracy = acc,
                    photoPath = photo,
                    qrDeployed = qrDep,
                    qrNotDeployedReason = qrReason,
                    merchantStatus = status,
                    paymentDiscussion = paymentDisc,
                    merchantResponse = resp,
                    comments = comments,
                    followUpRequired = followUp,
                    followUpDate = followUpDate
                )
                viewModel.startVisitForMerchant(null)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Visit submitted and locked successfully!")
                }
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "QR Friend • ${user.name}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextHeadings
                                    )
                                )
                                Text(
                                    text = "${region?.name ?: "Field Force"} • TL: ${tl?.name ?: "HQ"}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    },
                    actions = {
                        // Connectivity / Offline badge
                        Surface(
                            color = if (isOfflineMode) ErrorBg else SuccessBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isOfflineMode) Color(0xFFFCA5A5) else Color(0xFF86EFAC)),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { viewModel.toggleOfflineMode(!isOfflineMode) }
                                .testTag("network_status_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (isOfflineMode) ErrorRed else SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOfflineMode) "OFFLINE (${unsyncedCount})" else "ONLINE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOfflineMode) ErrorText else SuccessText
                                    )
                                )
                            }
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
                    tonalElevation = 6.dp,
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
                            val isVisitsTab = tab == BdoTab.VISITS

                            Surface(
                                color = if (isSelected) NavItemSelected else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { viewModel.setBdoTab(tab) }
                                    .testTag("bdo_nav_${label.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isVisitsTab && unsyncedCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = ErrorRed) {
                                                    Text("$unsyncedCount", color = Color.White)
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
                BdoTab.HOME -> BdoHomeScreen(
                    bdoUser = user,
                    merchants = assignedMerchants,
                    visits = bdoVisits,
                    target = todayTarget,
                    onStartVisit = { viewModel.startVisitForMerchant(it) },
                    onViewMerchant = { viewModel.viewMerchantDetail(it) },
                    onViewAllMerchants = { viewModel.setBdoTab(BdoTab.MERCHANTS) }
                )
                BdoTab.MERCHANTS -> BdoMerchantsScreen(
                    merchants = assignedMerchants,
                    onStartVisit = { viewModel.startVisitForMerchant(it) },
                    onViewDetail = { viewModel.viewMerchantDetail(it) }
                )
                BdoTab.VISITS -> BdoVisitsScreen(
                    visits = bdoVisits,
                    merchants = assignedMerchants
                )
                BdoTab.QR_REQUESTS -> QrOperationsScreen(
                    qrRequests = qrRequests.filter { it.bdoId == user.id },
                    merchants = assignedMerchants,
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
                BdoTab.TARGETS -> BdoTargetsScreen(
                    bdoUser = user,
                    target = todayTarget,
                    visits = bdoVisits
                )
                BdoTab.PROFILE -> BdoProfileScreen(
                    bdoUser = user,
                    region = region,
                    tl = tl,
                    isOfflineMode = isOfflineMode,
                    unsyncedCount = unsyncedCount,
                    onToggleOfflineMode = { viewModel.toggleOfflineMode(it) },
                    onSyncNow = {
                        viewModel.syncOfflineVisits()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Synced all offline visits to server!")
                        }
                    },
                    onLogout = { viewModel.logout() }
                )
            }

            // Merchant Detail Dialog (if tapped by BDO)
            if (selectedMerchantForDetail != null) {
                MerchantDetailDialog(
                    merchant = selectedMerchantForDetail!!,
                    visits = bdoVisits,
                    transactions = allTransactions,
                    users = listOf(user),
                    regions = regions,
                    tls = tls,
                    onDismiss = { viewModel.viewMerchantDetail(null) },
                    onViewVisit = {}
                )
            }
        }
    }
}
