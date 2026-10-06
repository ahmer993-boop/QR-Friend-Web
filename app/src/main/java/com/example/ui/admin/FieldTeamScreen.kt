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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.local.entity.TargetEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldTeamScreen(
    users: List<UserEntity>,
    regions: List<RegionEntity>,
    tls: List<TlEntity>,
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>,
    targets: List<TargetEntity>,
    onCreateRegion: (String) -> Unit,
    onCreateTl: (String, Long) -> Unit,
    onCreateBdo: (String, String, String, Long, Long) -> Unit
) {
    var showAddRegionDialog by remember { mutableStateOf(false) }
    var showAddTlDialog by remember { mutableStateOf(false) }
    var showAddBdoDialog by remember { mutableStateOf(false) }

    val bdos = users.filter { it.role == "BDO" }
    val today = DateUtils.getTodayDateString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("field_team_screen"),
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
                    text = "Field Force Hierarchy & Agents",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                )
                Text(
                    text = "Region → Team Leader (TL) → Business Development Officer (BDO)",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }
        }

        // Action Buttons to Add Region, TL, BDO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showAddRegionDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Region", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = { showAddTlDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add TL", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { showAddBdoDialog = true },
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add BDO", style = MaterialTheme.typography.bodySmall)
            }
        }

        // List of BDOs with Regional Hierarchy & Live Performance
        Text(
            text = "Active Field Agents (${bdos.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bdos, key = { it.id }) { bdo ->
                val region = regions.find { it.id == bdo.regionId }?.name ?: "North Region"
                val tl = tls.find { it.id == bdo.tlId }?.name ?: "Ahmed Khan"
                val assignedMerchants = merchants.filter { it.bdoId == bdo.id }
                val bdoVisitsToday = visits.filter { it.bdoId == bdo.id && it.visitDateString == today }
                val bdoOnboardings = visits.filter { it.bdoId == bdo.id && it.visitType.equals("New Onboarding", ignoreCase = true) }
                val bdoQrDeployments = visits.filter { it.bdoId == bdo.id && it.qrDeployed }
                val target = targets.find { it.bdoId == bdo.id && it.dateString == today }

                BdoHierarchyCard(
                    bdo = bdo,
                    regionName = region,
                    tlName = tl,
                    assignedCount = assignedMerchants.size,
                    todayVisitsCount = bdoVisitsToday.size,
                    onboardingsCount = bdoOnboardings.size,
                    qrDeploymentsCount = bdoQrDeployments.size,
                    target = target
                )
            }
        }
    }

    // Dialog: Add Region
    if (showAddRegionDialog) {
        var regionName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRegionDialog = false },
            title = { Text("Add New Region") },
            text = {
                OutlinedTextField(
                    value = regionName,
                    onValueChange = { regionName = it },
                    label = { Text("Region Name (e.g. East Region)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (regionName.isNotBlank()) {
                            onCreateRegion(regionName)
                            showAddRegionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Create Region")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRegionDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Add TL
    if (showAddTlDialog) {
        var tlName by remember { mutableStateOf("") }
        var selectedRegionId by remember { mutableStateOf(regions.firstOrNull()?.id ?: 1L) }
        AlertDialog(
            onDismissRequest = { showAddTlDialog = false },
            title = { Text("Add Team Leader (TL)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tlName,
                        onValueChange = { tlName = it },
                        label = { Text("TL Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    var regExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = regExpanded,
                        onExpandedChange = { regExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = regions.find { it.id == selectedRegionId }?.name ?: "Select Region",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign Region") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = regExpanded,
                            onDismissRequest = { regExpanded = false }
                        ) {
                            regions.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name) },
                                    onClick = {
                                        selectedRegionId = r.id
                                        regExpanded = false
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
                        if (tlName.isNotBlank()) {
                            onCreateTl(tlName, selectedRegionId)
                            showAddTlDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Create TL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTlDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Add BDO
    if (showAddBdoDialog) {
        var bdoName by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("+92 3") }
        var selectedRegionId by remember { mutableStateOf(regions.firstOrNull()?.id ?: 1L) }
        var selectedTlId by remember { mutableStateOf(tls.firstOrNull()?.id ?: 1L) }

        AlertDialog(
            onDismissRequest = { showAddBdoDialog = false },
            title = { Text("Add BDO Agent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bdoName,
                        onValueChange = { bdoName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username (for Login)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Region Dropdown
                    var regExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = regExpanded,
                        onExpandedChange = { regExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = regions.find { it.id == selectedRegionId }?.name ?: "Select Region",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign Region") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = regExpanded,
                            onDismissRequest = { regExpanded = false }
                        ) {
                            regions.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name) },
                                    onClick = {
                                        selectedRegionId = r.id
                                        regExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // TL Dropdown
                    var tlExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = tlExpanded,
                        onExpandedChange = { tlExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = tls.find { it.id == selectedTlId }?.name ?: "Select TL",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign Team Leader") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tlExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = tlExpanded,
                            onDismissRequest = { tlExpanded = false }
                        ) {
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

                    Text(
                        text = "Default password will be set to: Bdo@12345",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bdoName.isNotBlank() && username.isNotBlank()) {
                            onCreateBdo(username, bdoName, mobile, selectedRegionId, selectedTlId)
                            showAddBdoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Create BDO")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBdoDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BdoHierarchyCard(
    bdo: UserEntity,
    regionName: String,
    tlName: String,
    assignedCount: Int,
    todayVisitsCount: Int,
    onboardingsCount: Int,
    qrDeploymentsCount: Int,
    target: TargetEntity?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlueSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = bdo.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        )
                        Text(
                            text = "@${bdo.username} • Mobile: ${bdo.mobile}",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                        )
                    }
                }

                Surface(
                    color = EmeraldSubtle,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hierarchy Breadcrumb
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.CorporateFare, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = regionName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Navy900))
                Text(text = "  ›  ", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                Icon(imageVector = Icons.Default.SupervisorAccount, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "TL: $tlName", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Performance metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Assigned Merchants", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                    Text(text = "$assignedCount shops", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Navy900))
                }
                Column {
                    Text(text = "Today's Visits", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                    Text(
                        text = "$todayVisitsCount / ${target?.visitTarget ?: 10}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    )
                }
                Column {
                    Text(text = "Onboarded", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                    Text(text = "$onboardingsCount shops", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldDark))
                }
                Column {
                    Text(text = "QR Deployed", style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                    Text(text = "$qrDeploymentsCount", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldDark))
                }
            }
        }
    }
}
