package com.example.ui.admin

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
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.*
import com.example.util.DateUtils
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MerchantListScreen(
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>,
    users: List<UserEntity>,
    regions: List<RegionEntity>,
    tls: List<TlEntity>,
    transactions: List<TransactionEntity>,
    onSelectMerchant: (MerchantEntity) -> Unit,
    onReassignMerchant: (Long, Long, Long, Long) -> Unit,
    onCreateMerchant: (MerchantEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedQrFilter by remember { mutableStateOf("All") }
    var selectedBdoFilter by remember { mutableStateOf<Long?>(null) }

    var reassignDialogMerchant by remember { mutableStateOf<MerchantEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val bdos = users.filter { it.role == "BDO" }

    val filtered = merchants.filter { m ->
        val queryMatches = searchQuery.isBlank() ||
                m.shopName.contains(searchQuery, ignoreCase = true) ||
                m.merchantName.contains(searchQuery, ignoreCase = true) ||
                m.merchantId.contains(searchQuery, ignoreCase = true) ||
                m.mobile.contains(searchQuery, ignoreCase = true) ||
                m.address.contains(searchQuery, ignoreCase = true)

        val statusMatches = selectedStatusFilter == "All" || m.merchantStatus.equals(selectedStatusFilter, ignoreCase = true)
        val qrMatches = selectedQrFilter == "All" || m.qrStatus.equals(selectedQrFilter, ignoreCase = true)
        val bdoMatches = selectedBdoFilter == null || m.bdoId == selectedBdoFilter

        queryMatches && statusMatches && qrMatches && bdoMatches
    }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg).testTag("merchant_list_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Merchant Directory",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextHeadings
                        )
                    )
                    Text(
                        text = "${filtered.size} of ${merchants.size} Merchants listed",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimaryBg,
                        contentColor = ButtonPrimaryText
                    ),
                    modifier = Modifier.testTag("add_merchant_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Merchant")
                }
            }

            // Search and Filters Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search text field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("merchant_search_input"),
                        placeholder = { Text("Search by Shop Name, Merchant ID, Phone, City...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            focusedLabelColor = BrandGreenDark,
                            cursorColor = BrandGreen
                        )
                    )

                    // Filters
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status Filter
                        val statuses = listOf("All", "New", "Onboarded", "Active", "Inactive", "Temporarily Closed", "Permanently Closed", "Refused")
                        var statusMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = statusMenuExpanded,
                            onExpandedChange = { statusMenuExpanded = it },
                            modifier = Modifier.width(160.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedStatusFilter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Merchant Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuExpanded) },
                                modifier = Modifier.menuAnchor(),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandGreen,
                                    focusedLabelColor = BrandGreenDark
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false }
                            ) {
                                statuses.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            selectedStatusFilter = st
                                            statusMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // QR Status Filter
                        val qrStatuses = listOf("All", "Not Deployed", "Deployed", "Active", "Replacement Required")
                        var qrMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = qrMenuExpanded,
                            onExpandedChange = { qrMenuExpanded = it },
                            modifier = Modifier.width(160.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedQrFilter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("QR Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qrMenuExpanded) },
                                modifier = Modifier.menuAnchor(),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandGreen,
                                    focusedLabelColor = BrandGreenDark
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = qrMenuExpanded,
                                onDismissRequest = { qrMenuExpanded = false }
                            ) {
                                qrStatuses.forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q) },
                                        onClick = {
                                            selectedQrFilter = q
                                            qrMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // BDO Filter
                        var bdoMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = bdoMenuExpanded,
                            onExpandedChange = { bdoMenuExpanded = it },
                            modifier = Modifier.width(160.dp)
                        ) {
                            OutlinedTextField(
                                value = bdos.find { it.id == selectedBdoFilter }?.name ?: "All BDOs",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("BDO") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bdoMenuExpanded) },
                                modifier = Modifier.menuAnchor(),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandGreen,
                                    focusedLabelColor = BrandGreenDark
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = bdoMenuExpanded,
                                onDismissRequest = { bdoMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All BDOs") },
                                    onClick = {
                                        selectedBdoFilter = null
                                        bdoMenuExpanded = false
                                    }
                                )
                                bdos.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b.name) },
                                        onClick = {
                                            selectedBdoFilter = b.id
                                            bdoMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Merchant Cards List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { merchant ->
                    val bdo = users.find { it.id == merchant.bdoId }?.name ?: "Unassigned"
                    val merchantVisits = visits.filter { it.merchantId == merchant.merchantId }
                    val lastVisit = merchantVisits.maxByOrNull { it.visitTime }

                    MerchantCard(
                        merchant = merchant,
                        bdoName = bdo,
                        lastVisit = lastVisit,
                        onView = { onSelectMerchant(merchant) },
                        onReassign = { reassignDialogMerchant = merchant }
                    )
                }
            }
        }

        // Reassign Merchant Dialog
        if (reassignDialogMerchant != null) {
            val m = reassignDialogMerchant!!
            var newBdoId by remember { mutableStateOf(m.bdoId) }
            var newTlId by remember { mutableStateOf(m.tlId) }
            var newRegionId by remember { mutableStateOf(m.regionId) }

            AlertDialog(
                onDismissRequest = { reassignDialogMerchant = null },
                title = { Text("Reassign Merchant: ${m.shopName}", color = TextHeadings) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Select new BDO agent to handle this merchant account:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        bdos.forEach { bdo ->
                            val isSelected = bdo.id == newBdoId
                            Surface(
                                color = if (isSelected) BrandGreenLight else CardWhite,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) BrandGreen else BorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        newBdoId = bdo.id
                                        bdo.tlId?.let { newTlId = it }
                                        bdo.regionId?.let { newRegionId = it }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AssignmentInd,
                                        contentDescription = null,
                                        tint = if (isSelected) BrandGreenDark else TextMuted
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = bdo.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) BrandGreenDark else TextHeadings
                                        )
                                        Text(
                                            text = "Username: ${bdo.username} • ${regions.find { it.id == bdo.regionId }?.name ?: ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onReassignMerchant(m.id, newBdoId, newTlId, newRegionId)
                            reassignDialogMerchant = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimaryBg,
                            contentColor = ButtonPrimaryText
                        )
                    ) {
                        Text("Confirm Reassignment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { reassignDialogMerchant = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Add Merchant Dialog
        if (showCreateDialog) {
            AddMerchantDialog(
                bdos = bdos,
                regions = regions,
                tls = tls,
                onDismiss = { showCreateDialog = false },
                onAdd = { merchant ->
                    onCreateMerchant(merchant)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun MerchantCard(
    merchant: MerchantEntity,
    bdoName: String,
    lastVisit: VisitEntity?,
    onView: () -> Unit,
    onReassign: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("merchant_card_${merchant.merchantId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with Shop Name and Status Badges
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

                // Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(status = merchant.merchantStatus)
                    QrBadge(status = merchant.qrStatus)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address and Phone
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${merchant.address}, ${merchant.city}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = merchant.mobile,
                    style = MaterialTheme.typography.bodySmall.copy(color = BrandGreenDark, fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "BDO: $bdoName",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextHeadings
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(8.dp))

            // Last Visit & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastVisit != null) {
                    Column {
                        Text(
                            text = "Last Visit: ${DateUtils.formatDateTime(lastVisit.visitTime)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = "Status: ${lastVisit.gpsStatus} (${GeoUtils.formatDistance(lastVisit.distanceMeters)})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (lastVisit.gpsStatus == "VALID") SuccessText else DangerText
                            )
                        )
                    }
                } else {
                    Text(
                        text = "No field visits recorded yet",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onReassign,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ButtonSecondaryBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonSecondaryText),
                        modifier = Modifier.testTag("reassign_button_${merchant.merchantId}")
                    ) {
                        Text("Reassign", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onView,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimaryBg,
                            contentColor = ButtonPrimaryText
                        ),
                        modifier = Modifier.testTag("view_merchant_${merchant.merchantId}")
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Details", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, text, border) = when (status.lowercase()) {
        "active" -> Triple(SuccessBg, SuccessText, Color(0xFF86EFAC))
        "onboarded" -> Triple(InfoBg, InfoText, Color(0xFFBAE6FD))
        "new" -> Triple(BrandGreenLight, BrandGreenDark, BrandGreen.copy(alpha = 0.3f))
        "temporarily closed" -> Triple(WarningBg, WarningOrange, Color(0xFFFDE68A))
        "permanently closed", "refused", "inactive" -> Triple(DangerBg, DangerText, Color(0xFFFCA5A5))
        else -> Triple(Color(0xFFF1F5F9), TextSecondary, BorderColor)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = text)
        )
    }
}

@Composable
fun QrBadge(status: String) {
    val (bg, text, border) = when (status.lowercase()) {
        "active", "deployed" -> Triple(SuccessBg, SuccessText, Color(0xFF86EFAC))
        "replacement required" -> Triple(WarningBg, WarningOrange, Color(0xFFFDE68A))
        else -> Triple(Color(0xFFF8FAFC), TextSecondary, BorderColor)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = text, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = text)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMerchantDialog(
    bdos: List<UserEntity>,
    regions: List<RegionEntity>,
    tls: List<TlEntity>,
    onDismiss: () -> Unit,
    onAdd: (MerchantEntity) -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var merchantName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Lahore") }
    var category by remember { mutableStateOf("General Store") }
    var latStr by remember { mutableStateOf("31.5204") }
    var lonStr by remember { mutableStateOf("74.3587") }
    var selectedBdoId by remember { mutableStateOf(bdos.firstOrNull()?.id ?: 1L) }

    val selectedBdo = bdos.find { it.id == selectedBdoId }
    val defaultRegionId = selectedBdo?.regionId ?: regions.firstOrNull()?.id ?: 1L
    val defaultTlId = selectedBdo?.tlId ?: tls.firstOrNull()?.id ?: 1L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Merchant", color = TextHeadings) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, focusedLabelColor = BrandGreenDark)
                )
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Owner / Merchant Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, focusedLabelColor = BrandGreenDark)
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Phone *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, focusedLabelColor = BrandGreenDark)
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, focusedLabelColor = BrandGreenDark)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, focusedLabelColor = BrandGreenDark)
                    )
                    OutlinedTextField(
                        value = lonStr,
                        onValueChange = { lonStr = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, focusedLabelColor = BrandGreenDark)
                    )
                }

                // BDO Dropdown
                var bdoExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = bdoExpanded,
                    onExpandedChange = { bdoExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = bdos.find { it.id == selectedBdoId }?.name ?: "Select BDO",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign BDO Agent") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bdoExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, focusedLabelColor = BrandGreenDark)
                    )
                    ExposedDropdownMenu(
                        expanded = bdoExpanded,
                        onDismissRequest = { bdoExpanded = false }
                    ) {
                        bdos.forEach { bdo ->
                            DropdownMenuItem(
                                text = { Text("${bdo.name} (${bdo.username})") },
                                onClick = {
                                    selectedBdoId = bdo.id
                                    bdoExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shopName.isNotBlank() && merchantName.isNotBlank()) {
                        val mId = "M-${(2000..9999).random()}"
                        val merchant = MerchantEntity(
                            merchantId = mId,
                            merchantName = merchantName.trim(),
                            shopName = shopName.trim(),
                            mobile = mobile.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            regionId = defaultRegionId,
                            tlId = defaultTlId,
                            bdoId = selectedBdoId,
                            latitude = latStr.toDoubleOrNull() ?: 31.5204,
                            longitude = lonStr.toDoubleOrNull() ?: 74.3587,
                            qrId = "",
                            qrStatus = "Not Deployed",
                            merchantStatus = "New",
                            merchantCategory = category,
                            onboardingDate = ""
                        )
                        onAdd(merchant)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimaryBg,
                    contentColor = ButtonPrimaryText
                )
            ) {
                Text("Save Merchant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
