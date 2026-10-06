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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.QrRequestEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RedSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.DateUtils
import com.example.util.ExcelExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrOperationsScreen(
    qrRequests: List<QrRequestEntity>,
    merchants: List<MerchantEntity>,
    users: List<UserEntity>,
    currentUser: UserEntity?,
    onCreateRequest: (QrRequestEntity) -> Unit,
    onForwardToVendor: (QrRequestEntity, String, String, String) -> Unit,
    onUpdateScanning: (QrRequestEntity, String, String) -> Unit,
    onUpdateCourier: (QrRequestEntity, String, String, String, String) -> Unit,
    onConfirmDelivery: (QrRequestEntity, String) -> Unit,
    onEscalate: (QrRequestEntity, String) -> Unit,
    onSubmitJustification: (QrRequestEntity, String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Dialog States
    var showCreateDialog by remember { mutableStateOf(false) }
    var requestForVendor by remember { mutableStateOf<QrRequestEntity?>(null) }
    var requestForScanning by remember { mutableStateOf<QrRequestEntity?>(null) }
    var requestForCourier by remember { mutableStateOf<QrRequestEntity?>(null) }
    var requestForDelivery by remember { mutableStateOf<QrRequestEntity?>(null) }
    var requestForEscalate by remember { mutableStateOf<QrRequestEntity?>(null) }
    var requestForJustification by remember { mutableStateOf<QrRequestEntity?>(null) }

    val filters = listOf(
        "ALL" to "All (${qrRequests.size})",
        "PENDING_VENDOR" to "Pending Vendor (${qrRequests.count { it.status == "PENDING_VENDOR" }})",
        "FORWARDED" to "At Vendor (${qrRequests.count { it.status == "FORWARDED_TO_VENDOR" }})",
        "SCANNED" to "Scanned (${qrRequests.count { it.status == "SCANNED" }})",
        "DISPATCHED" to "In Transit (${qrRequests.count { it.status == "DISPATCHED" }})",
        "DELIVERED" to "Delivered / At Hand (${qrRequests.count { it.status == "DELIVERED" }})",
        "ESCALATED" to "Escalations (${qrRequests.count { it.isEscalated || it.status == "ESCALATED" }})"
    )

    val filteredList = qrRequests.filter { req ->
        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "PENDING_VENDOR" -> req.status == "PENDING_VENDOR"
            "FORWARDED" -> req.status == "FORWARDED_TO_VENDOR"
            "SCANNED" -> req.status == "SCANNED"
            "DISPATCHED" -> req.status == "DISPATCHED"
            "DELIVERED" -> req.status == "DELIVERED"
            "ESCALATED" -> req.isEscalated || req.status == "ESCALATED"
            else -> true
        }
        val matchesQuery = searchQuery.isBlank() ||
                req.requestId.contains(searchQuery, ignoreCase = true) ||
                req.merchantName.contains(searchQuery, ignoreCase = true) ||
                req.merchantId.contains(searchQuery, ignoreCase = true) ||
                req.bdoName.contains(searchQuery, ignoreCase = true) ||
                req.trackingNumber.contains(searchQuery, ignoreCase = true)

        matchesFilter && matchesQuery
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header & Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "QR Printing, Reprint & Deployment Workflow",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        )
                        Text(
                            text = "Complete operational cycle: Request → Vendor → Physical Scanning → Dispatch → Delivery → Qualified Deployment (>= Rs. 6,000)",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val file = ExcelExporter.exportQrReprintReport(context, qrRequests)
                                ExcelExporter.shareOrOpenFile(context, file, "QR Reprint Tracking Report")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("export_qr_report_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Report")
                        }

                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("new_qr_request_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New QR Request")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // KPI Counter Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiStatCard("Total Requests", qrRequests.size.toString(), PrimaryBlue, Modifier.weight(1f))
                    KpiStatCard("At Vendor", qrRequests.count { it.status == "FORWARDED_TO_VENDOR" }.toString(), Color(0xFFD97706), Modifier.weight(1f))
                    KpiStatCard("In Transit", qrRequests.count { it.status == "DISPATCHED" }.toString(), Color(0xFF2563EB), Modifier.weight(1f))
                    KpiStatCard("Delivered", qrRequests.count { it.status == "DELIVERED" }.toString(), EmeraldDark, Modifier.weight(1f))
                    KpiStatCard("Escalated", qrRequests.count { it.isEscalated }.toString(), RedDanger, Modifier.weight(1f))
                }
            }
        }

        // Search and Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Request ID, Merchant, BDO, Tracking #") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("qr_search_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }

        // Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { (key, label) ->
                val isSelected = selectedFilter == key
                Surface(
                    color = if (isSelected) PrimaryBlue else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clickable { selectedFilter = key }
                        .testTag("filter_qr_$key")
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Navy900
                        )
                    )
                }
            }
        }

        // Requests List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = SlateMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No QR Requests Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Text(
                        text = "Create a new printing or reprint request above to start tracking.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { req ->
                    QrRequestCardItem(
                        request = req,
                        onVendorClick = { requestForVendor = req },
                        onScanningClick = { requestForScanning = req },
                        onCourierClick = { requestForCourier = req },
                        onDeliveryClick = { requestForDelivery = req },
                        onEscalateClick = { requestForEscalate = req },
                        onJustificationClick = { requestForJustification = req }
                    )
                }
            }
        }
    }

    // Dialog 1: Create New Request
    if (showCreateDialog) {
        CreateQrRequestDialog(
            merchants = merchants,
            users = users,
            currentUser = currentUser,
            onDismiss = { showCreateDialog = false },
            onCreate = { newReq ->
                onCreateRequest(newReq)
                showCreateDialog = false
            }
        )
    }

    // Dialog 2: Forward to Vendor
    if (requestForVendor != null) {
        val req = requestForVendor!!
        ForwardToVendorDialog(
            request = req,
            currentUser = currentUser,
            onDismiss = { requestForVendor = null },
            onConfirm = { vDate, vRef, coord ->
                onForwardToVendor(req, vDate, vRef, coord)
                requestForVendor = null
            }
        )
    }

    // Dialog 3: Physical Scanning Verification
    if (requestForScanning != null) {
        val req = requestForScanning!!
        ScanningVerificationDialog(
            request = req,
            onDismiss = { requestForScanning = null },
            onConfirm = { status, disc ->
                onUpdateScanning(req, status, disc)
                requestForScanning = null
            }
        )
    }

    // Dialog 4: Dispatch & Courier Details
    if (requestForCourier != null) {
        val req = requestForCourier!!
        DispatchCourierDialog(
            request = req,
            onDismiss = { requestForCourier = null },
            onConfirm = { courier, tracking, dispatchDate, expectedDate ->
                onUpdateCourier(req, courier, tracking, dispatchDate, expectedDate)
                requestForCourier = null
            }
        )
    }

    // Dialog 5: Confirm Delivery
    if (requestForDelivery != null) {
        val req = requestForDelivery!!
        ConfirmDeliveryDialog(
            request = req,
            onDismiss = { requestForDelivery = null },
            onConfirm = { deliveryDate ->
                onConfirmDelivery(req, deliveryDate)
                requestForDelivery = null
            }
        )
    }

    // Dialog 6: Escalate
    if (requestForEscalate != null) {
        val req = requestForEscalate!!
        EscalateRequestDialog(
            request = req,
            onDismiss = { requestForEscalate = null },
            onConfirm = { comments ->
                onEscalate(req, comments)
                requestForEscalate = null
            }
        )
    }

    // Dialog 7: Non-Deployment Justification
    if (requestForJustification != null) {
        val req = requestForJustification!!
        NonDeploymentJustificationDialog(
            request = req,
            onDismiss = { requestForJustification = null },
            onConfirm = { justification ->
                onSubmitJustification(req, justification)
                requestForJustification = null
            }
        )
    }
}

@Composable
private fun KpiStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = color))
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
        }
    }
}

@Composable
private fun QrRequestCardItem(
    request: QrRequestEntity,
    onVendorClick: () -> Unit,
    onScanningClick: () -> Unit,
    onCourierClick: () -> Unit,
    onDeliveryClick: () -> Unit,
    onEscalateClick: () -> Unit,
    onJustificationClick: () -> Unit
) {
    val statusColor = when (request.status) {
        "PENDING_VENDOR" -> Color(0xFFD97706)
        "FORWARDED_TO_VENDOR" -> Color(0xFF2563EB)
        "SCANNED" -> Color(0xFF7C3AED)
        "DISPATCHED" -> Color(0xFF0284C7)
        "DELIVERED" -> EmeraldDark
        "ESCALATED", "DISPUTED" -> RedDanger
        else -> SlateMuted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (request.requestType == "NEW_PRINTING") Color(0xFFEFF6FF) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (request.requestType == "NEW_PRINTING") "NEW PRINTING" else "REPRINT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (request.requestType == "NEW_PRINTING") Color(0xFF2563EB) else Color(0xFFD97706)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = request.requestId,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )

                    if (request.isUrgent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = RedSubtle,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "URGENT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RedDanger,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = request.status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Merchant & Field Force Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${request.merchantName} (${request.merchantId})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Text(
                        text = "City: ${request.city.ifBlank { "N/A" }} • Reason: ${request.reason}",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                    )
                    Text(
                        text = "Field Force: BDO ${request.bdoName} • TL ${request.tlName} • ASM ${request.asmName}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF475569))
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Requested: ${request.requestDate} ${request.requestTime}",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                    )
                    if (request.courierName.isNotBlank()) {
                        Text(
                            text = "${request.courierName} #${request.trackingNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                        )
                    }
                }
            }

            // Tracking Milestones Summary
            if (request.vendorReference.isNotBlank() || request.scanningStatus.isNotBlank() || request.isEscalated) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = SlateBorder)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (request.vendorReference.isNotBlank()) {
                        Text(
                            text = "Vendor Ref: ${request.vendorReference} (${request.vendorForwardedDate})",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                        )
                    }
                    if (request.scanningStatus.isNotBlank()) {
                        Text(
                            text = "Scan: ${request.scanningStatus}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (request.scanningDiscrepancy.isNotBlank()) RedDanger else EmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    if (request.isEscalated) {
                        Text(
                            text = "Escalated: ${request.escalationComments}",
                            style = MaterialTheme.typography.labelSmall.copy(color = RedDanger, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Non Deployment Justification if present
            if (request.nonDeploymentJustification.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Delay Justification (${request.justificationUser}): ${request.nonDeploymentJustification}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF92400E)),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Workflow Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (request.status == "PENDING_VENDOR") {
                    Button(
                        onClick = onVendorClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Forward to Vendor", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (request.status == "FORWARDED_TO_VENDOR") {
                    Button(
                        onClick = onScanningClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Physical Scan Check", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (request.status == "SCANNED") {
                    Button(
                        onClick = onCourierClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispatch Courier", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (request.status == "DISPATCHED") {
                    Button(
                        onClick = onDeliveryClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm Delivery", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Justification Button
                OutlinedButton(
                    onClick = onJustificationClick,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Justification", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Escalate Button
                if (!request.isEscalated) {
                    OutlinedButton(
                        onClick = onEscalateClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escalate", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQrRequestDialog(
    merchants: List<MerchantEntity>,
    users: List<UserEntity>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onCreate: (QrRequestEntity) -> Unit
) {
    var requestType by remember { mutableStateOf("NEW_PRINTING") }
    var selectedMerchant by remember { mutableStateOf<MerchantEntity?>(null) }
    var merchantExpanded by remember { mutableStateOf(false) }
    var merchantSearch by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Damaged / Unreadable QR") }
    var isUrgent by remember { mutableStateOf(false) }
    var urgentNotes by remember { mutableStateOf("") }

    val reasons = listOf(
        "Damaged / Unreadable QR",
        "QR Code Missing at Store",
        "Faded / Scratched QR Code",
        "Incorrect Business Name on QR",
        "Wrong Linked Mobile Number",
        "Tampered / Altered QR Code",
        "Merchant Requested Replacement"
    )

    val filteredMerchants = merchants.filter {
        merchantSearch.isBlank() ||
                it.merchantName.contains(merchantSearch, ignoreCase = true) ||
                it.merchantId.contains(merchantSearch, ignoreCase = true) ||
                it.shopName.contains(merchantSearch, ignoreCase = true)
    }.take(10)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create QR Printing / Reprint Request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Request Type Toggle
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = if (requestType == "NEW_PRINTING") PrimaryBlue else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { requestType = "NEW_PRINTING" }
                            .weight(1f)
                    ) {
                        Text(
                            text = "New Printing",
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (requestType == "NEW_PRINTING") Color.White else Navy900
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Surface(
                        color = if (requestType == "REPRINT") Color(0xFFD97706) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { requestType = "REPRINT" }
                            .weight(1f)
                    ) {
                        Text(
                            text = "External Reprint",
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (requestType == "REPRINT") Color.White else Navy900
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Merchant Selector
                Text("Select Merchant:", style = MaterialTheme.typography.labelSmall)
                ExposedDropdownMenuBox(
                    expanded = merchantExpanded,
                    onExpandedChange = { merchantExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMerchant?.let { "${it.shopName.ifBlank { it.merchantName }} (${it.merchantId})" } ?: merchantSearch,
                        onValueChange = {
                            merchantSearch = it
                            selectedMerchant = null
                            merchantExpanded = true
                        },
                        label = { Text("Search Merchant Name or ID") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = merchantExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = merchantExpanded,
                        onDismissRequest = { merchantExpanded = false }
                    ) {
                        filteredMerchants.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.shopName.ifBlank { m.merchantName }} • ${m.merchantId} (${m.city})") },
                                onClick = {
                                    selectedMerchant = m
                                    merchantExpanded = false
                                }
                            )
                        }
                    }
                }

                // Reason Selection
                Text("Reason for Request:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reasons) { r ->
                        val isSel = reason == r
                        Surface(
                            color = if (isSel) PrimaryBlue else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { reason = r }
                        ) {
                            Text(
                                text = r,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(color = if (isSel) Color.White else Navy900)
                            )
                        }
                    }
                }

                // Urgent Flag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isUrgent,
                        onCheckedChange = { isUrgent = it }
                    )
                    Text("Urgent Request (Requires ASM Priority Approval)", style = MaterialTheme.typography.bodySmall)
                }

                if (isUrgent) {
                    OutlinedTextField(
                        value = urgentNotes,
                        onValueChange = { urgentNotes = it },
                        label = { Text("Urgent Justification & Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = selectedMerchant ?: return@Button
                    val now = System.currentTimeMillis()
                    val reqId = "QR-${if (requestType == "NEW_PRINTING") "PRN" else "REP"}-${DateUtils.getTodayDateString().replace("-", "")}-${(now % 10000).toString().padStart(4, '0')}"

                    val bdo = users.find { it.id == m.bdoId }
                    val tl = users.find { it.id == m.tlId }
                    val asm = users.find { it.id == m.asmId }

                    val req = QrRequestEntity(
                        requestId = reqId,
                        requestType = requestType,
                        merchantId = m.merchantId,
                        merchantName = m.shopName.ifBlank { m.merchantName },
                        city = m.city,
                        regionId = m.regionId ?: 1L,
                        regionName = m.city,
                        bdoId = m.bdoId ?: currentUser?.id ?: 0L,
                        bdoName = bdo?.name ?: currentUser?.name ?: "BDO",
                        tlId = m.tlId ?: 0L,
                        tlName = tl?.name ?: "TL",
                        asmId = m.asmId ?: 0L,
                        asmName = asm?.name ?: "ASM",
                        reason = reason,
                        isUrgent = isUrgent,
                        urgentApprovalNotes = urgentNotes,
                        requestDate = DateUtils.getTodayDateString(),
                        requestTime = DateUtils.formatTime(now),
                        status = "PENDING_VENDOR"
                    )
                    onCreate(req)
                },
                enabled = selectedMerchant != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Submit QR Request")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ForwardToVendorDialog(
    request: QrRequestEntity,
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var vendorDate by remember { mutableStateOf(DateUtils.getTodayDateString()) }
    var vendorRef by remember { mutableStateOf("VND-${System.currentTimeMillis() % 100000}") }
    var coordinatorName by remember { mutableStateOf(currentUser?.name ?: "Operations Coordinator") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forward to Printing Vendor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Request: ${request.requestId} for ${request.merchantName}")
                OutlinedTextField(
                    value = vendorDate,
                    onValueChange = { vendorDate = it },
                    label = { Text("Vendor Submission Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vendorRef,
                    onValueChange = { vendorRef = it },
                    label = { Text("Vendor Job / Batch Reference Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coordinatorName,
                    onValueChange = { coordinatorName = it },
                    label = { Text("Coordinator Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(vendorDate, vendorRef, coordinatorName) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Confirm Forwarding")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ScanningVerificationDialog(
    request: QrRequestEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var hasMismatch by remember { mutableStateOf(false) }
    var discrepancyNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Physical QR Scanning & Verification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Physical Scan Cross-Check against Record:")
                Text("• Merchant ID: ${request.merchantId}", fontWeight = FontWeight.SemiBold)
                Text("• Merchant Name: ${request.merchantName}", fontWeight = FontWeight.SemiBold)
                Text("• City / Region: ${request.city}", fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasMismatch,
                        onCheckedChange = { hasMismatch = it }
                    )
                    Text("Flag Mismatch / Printing Discrepancy", style = MaterialTheme.typography.bodySmall, color = RedDanger)
                }

                if (hasMismatch) {
                    OutlinedTextField(
                        value = discrepancyNotes,
                        onValueChange = { discrepancyNotes = it },
                        label = { Text("Discrepancy Details (e.g. spelling error, wrong QR string)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(color = EmeraldSubtle, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "✓ QR barcode string matches database record exactly.",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldDark),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val status = if (hasMismatch) "DISPUTED" else "SCANNED_PASSED"
                    onConfirm(status, discrepancyNotes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (hasMismatch) RedDanger else EmeraldDark)
            ) {
                Text(if (hasMismatch) "Flag Discrepancy" else "Pass Physical Verification")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DispatchCourierDialog(
    request: QrRequestEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var courierName by remember { mutableStateOf("TCS Courier") }
    var trackingNumber by remember { mutableStateOf("TRK-${System.currentTimeMillis() % 1000000}") }
    var dispatchDate by remember { mutableStateOf(DateUtils.getTodayDateString()) }
    var expectedDate by remember { mutableStateOf(DateUtils.formatDate(System.currentTimeMillis() + 3 * 24 * 3600 * 1000L)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispatch & Courier Assignment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dispatching QR for: ${request.merchantName}")
                OutlinedTextField(
                    value = courierName,
                    onValueChange = { courierName = it },
                    label = { Text("Courier / Logistics Service") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = trackingNumber,
                    onValueChange = { trackingNumber = it },
                    label = { Text("Waybill / Tracking Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dispatchDate,
                    onValueChange = { dispatchDate = it },
                    label = { Text("Dispatch Date") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expectedDate,
                    onValueChange = { expectedDate = it },
                    label = { Text("Expected Delivery Date") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(courierName, trackingNumber, dispatchDate, expectedDate) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Record Dispatch")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmDeliveryDialog(
    request: QrRequestEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var deliveryDate by remember { mutableStateOf(DateUtils.getTodayDateString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Physical QR Handover") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Confirm delivery of physical QR to BDO ${request.bdoName} for ${request.merchantName}.")
                Text(
                    text = "Important: Upon delivery, QR status becomes 'At Hand'. Deployment is ONLY marked after a qualifying transaction of Rs. 6,000+ is recorded.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD97706))
                )
                OutlinedTextField(
                    value = deliveryDate,
                    onValueChange = { deliveryDate = it },
                    label = { Text("Actual Delivery Date") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deliveryDate) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("Confirm Handover (QR At Hand)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EscalateRequestDialog(
    request: QrRequestEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var comments by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escalate QR Request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escalate request ${request.requestId} for ${request.merchantName} to Regional Management & Master Controller.")
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Escalation Reason / Comments") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (comments.isNotBlank()) onConfirm(comments) },
                enabled = comments.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
            ) {
                Text("Submit Escalation")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NonDeploymentJustificationDialog(
    request: QrRequestEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var justification by remember { mutableStateOf("") }
    val reasons = listOf(
        "Shop was temporarily closed during delivery attempt",
        "Merchant owner unavailable / out of station",
        "Awaiting qualifying transaction (>= Rs. 6,000)",
        "Merchant requested visit rescheduling",
        "Signboard installation pending by merchant"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Delay / Non-Deployment Justification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mandatory for delayed or overdue deployments to prevent automated audit penalty.")

                Text("Quick Reasons:", style = MaterialTheme.typography.labelSmall)
                reasons.forEach { r ->
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { justification = r }
                    ) {
                        Text(
                            text = r,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = Navy900)
                        )
                    }
                }

                OutlinedTextField(
                    value = justification,
                    onValueChange = { justification = it },
                    label = { Text("Justification Explanation") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (justification.isNotBlank()) onConfirm(justification) },
                enabled = justification.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Submit Justification")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
