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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.common.MasterTab
import com.example.ui.theme.*
import com.example.util.SecurityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterMainScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentTab by viewModel.masterTab.collectAsState()
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
    val userActionMessage by viewModel.userActionMessage.collectAsState()
    val cloudStatus by viewModel.cloudStatus.collectAsState()

    val selectedMerchantForDetail by viewModel.selectedMerchantForDetail.collectAsState()
    val selectedVisitForDetail by viewModel.selectedVisitForDetail.collectAsState()

    val tabs = listOf(
        Triple(MasterTab.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
        Triple(MasterTab.HIERARCHY, Icons.Default.AccountTree, "Merchant Allocation"),
        Triple(MasterTab.USERS, Icons.Default.ManageAccounts, "Manage Accounts"),
        Triple(MasterTab.QR_OPERATIONS, Icons.Default.QrCodeScanner, "QR Operations"),
        Triple(MasterTab.MERCHANTS, Icons.Default.Store, "Merchant Management"),
        Triple(MasterTab.EXCEL_UPLOAD, Icons.Default.FileUpload, "Upload Merchant Excel"),
        Triple(MasterTab.VISITS, Icons.Default.Visibility, "Field Visits"),
        Triple(MasterTab.MAP_VIEW, Icons.Default.Map, "Map"),
        Triple(MasterTab.REPORTS, Icons.Default.Assessment, "Reports"),
        Triple(MasterTab.AUDIT_LOGS, Icons.Default.History, "Audit Logs"),
        Triple(MasterTab.SETTINGS, Icons.Default.Settings, "Settings")
    )

    Scaffold(
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
                                    imageVector = Icons.Default.Shield,
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
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextHeadings
                                    )
                                )
                                Text(
                                    text = "Master Control Center • System Owner",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            color = BrandGreenLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentUser?.name ?: "Master",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("master_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = White,
                        titleContentColor = TextHeadings,
                        actionIconContentColor = TextSecondary
                    )
                )
                // Top Navigation Task Bar (Moved to top as requested)
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
                                    .clickable { viewModel.setMasterTab(tab) }
                                    .testTag("master_tab_${tab.name.lowercase()}")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (userActionMessage != null) {
                val isError = userActionMessage!!.startsWith("Error")
                Surface(
                    color = if (isError) RedSubtle else BrandGreenSubtle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) RedDanger else BrandGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = userActionMessage!!,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isError) RedDanger else BrandGreenDark,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearUserActionMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = if (isError) RedDanger else BrandGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentTab) {
                MasterTab.DASHBOARD -> AdminDashboardScreen(
                    merchants = merchants,
                    visits = visits,
                    users = users,
                    regions = regions,
                    tls = tls,
                    transactions = transactions,
                    onNavigateTab = { /* no-op */ },
                    onSelectVisit = { viewModel.viewVisitDetail(it) }
                )
                MasterTab.HIERARCHY -> OrgHierarchyScreen(
                    regions = regions,
                    users = users,
                    merchants = merchants,
                    visits = visits,
                    onCreateAsm = { name, uName, mob, rId ->
                        val hash = SecurityUtils.hashPassword("Asm@12345")
                        viewModel.createUser(
                            UserEntity(
                                username = uName.trim().lowercase(),
                                passwordHash = hash,
                                role = "ASM",
                                name = name.trim(),
                                mobile = mob.trim(),
                                regionId = rId,
                                status = "Active"
                            )
                        )
                    },
                    onCreateTl = { name, uName, mob, rId, asmId ->
                        val hash = SecurityUtils.hashPassword("Tl@12345")
                        viewModel.createUser(
                            UserEntity(
                                username = uName.trim().lowercase(),
                                passwordHash = hash,
                                role = "TL",
                                name = name.trim(),
                                mobile = mob.trim(),
                                regionId = rId,
                                asmId = asmId,
                                status = "Active"
                            )
                        )
                    },
                    onCreateBdo = { name, uName, mob, rId, asmId, tlId ->
                        val hash = SecurityUtils.hashPassword("Bdo@12345")
                        viewModel.createUser(
                            UserEntity(
                                username = uName.trim().lowercase(),
                                passwordHash = hash,
                                role = "BDO",
                                name = name.trim(),
                                mobile = mob.trim(),
                                regionId = rId,
                                asmId = asmId,
                                tlId = tlId,
                                status = "Active"
                            )
                        )
                    }
                )
                MasterTab.USERS -> MasterUserManagementScreen(
                    users = users,
                    regions = regions,
                    onToggleStatus = { uId, newStatus -> viewModel.updateUserStatus(uId, newStatus) },
                    onReassign = { uId, tlId, asmId, rId -> viewModel.reassignUserHierarchy(uId, tlId, asmId, rId) },
                    onResetPassword = { uId, pass -> viewModel.resetUserPassword(uId, pass) },
                    onEditUser = { viewModel.updateUser(it) },
                    onDeleteUser = { viewModel.deleteUser(it) },
                    onCreateUser = { u -> viewModel.createUser(u) }
                )
                MasterTab.QR_OPERATIONS -> QrOperationsScreen(
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
                MasterTab.MERCHANTS -> MerchantListScreen(
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
                MasterTab.EXCEL_UPLOAD -> ExcelUploadScreen(
                    existingMerchants = merchants,
                    users = users,
                    regions = regions,
                    tls = tls,
                    onImportMerchants = { list, onDone ->
                        viewModel.importMerchants(list, onDone)
                    }
                )
                MasterTab.VISITS -> AdminVisitsScreen(
                    visits = visits,
                    merchants = merchants,
                    users = users,
                    onSelectVisit = { viewModel.viewVisitDetail(it) }
                )
                MasterTab.MAP_VIEW -> MapViewScreen(
                    visits = visits,
                    merchants = merchants,
                    users = users,
                    onSelectVisit = { viewModel.viewVisitDetail(it) }
                )
                MasterTab.REPORTS -> ReportsScreen(
                    merchants = merchants,
                    visits = visits,
                    users = users,
                    targets = targets,
                    transactions = transactions
                )
                MasterTab.AUDIT_LOGS -> AuditLogScreen(auditLogs = auditLogs)
                MasterTab.SETTINGS -> SettingsScreen(
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
}

@Composable
fun AuditLogScreen(auditLogs: List<AuditLogEntity>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Audit Trail",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Navy900)
                )
                Text(
                    text = "Immutable chronological record of administrative actions, role assignments, imports, and system adjustments.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(auditLogs.sortedByDescending { it.timestamp }) { log ->
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
                            Surface(
                                color = Color(0xFFEDE9FE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = log.action,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6D28D9)
                                    )
                                )
                            }
                            Text(
                                text = "${log.dateStr} ${log.timeStr}",
                                style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Actor: ${log.userName} (User #${log.userId})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Navy900)
                        )

                        if (log.recordAffected.isNotBlank()) {
                            Text(
                                text = "Affected: ${log.recordAffected}",
                                style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                            )
                        }

                        if (log.metadata.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.metadata,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                            )
                        }

                        if (log.newValue.isNotBlank() && log.newValue != "None") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Change: ${log.previousValue} → ${log.newValue}",
                                style = MaterialTheme.typography.labelSmall.copy(color = EmeraldDark, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}
