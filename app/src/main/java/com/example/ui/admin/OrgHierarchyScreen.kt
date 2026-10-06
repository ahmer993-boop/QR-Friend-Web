package com.example.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted

@Composable
fun OrgHierarchyScreen(
    regions: List<RegionEntity>,
    users: List<UserEntity>,
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>,
    onCreateAsm: (name: String, uName: String, mob: String, rId: Long) -> Unit,
    onCreateTl: (name: String, uName: String, mob: String, rId: Long, asmId: Long) -> Unit,
    onCreateBdo: (name: String, uName: String, mob: String, rId: Long, asmId: Long, tlId: Long) -> Unit
) {
    var showAddAsmDialogForRegion by remember { mutableStateOf<RegionEntity?>(null) }
    var showAddTlDialogForAsm by remember { mutableStateOf<UserEntity?>(null) }
    var showAddBdoDialogForTl by remember { mutableStateOf<UserEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                            text = "5-Tier Organizational Hierarchy",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        )
                        Text(
                            text = "Master → Admin → Area Sales Manager (ASM) → Team Leader (TL) → Field Officer (BDO)",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(regions, key = { it.id }) { region ->
                val regionAsms = users.filter { it.role == "ASM" && it.regionId == region.id }
                val regionMerchants = merchants.filter { it.regionId == region.id }

                RegionHierarchyCard(
                    region = region,
                    asms = regionAsms,
                    allUsers = users,
                    merchantsCount = regionMerchants.size,
                    onAddAsm = { showAddAsmDialogForRegion = region },
                    onAddTl = { asm -> showAddTlDialogForAsm = asm },
                    onAddBdo = { tl -> showAddBdoDialogForTl = tl }
                )
            }
        }
    }

    // Add ASM Dialog
    if (showAddAsmDialogForRegion != null) {
        val reg = showAddAsmDialogForRegion!!
        var asmName by remember { mutableStateOf("") }
        var asmUsername by remember { mutableStateOf("") }
        var asmMobile by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAsmDialogForRegion = null },
            title = { Text("Add ASM for ${reg.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = asmName,
                        onValueChange = { asmName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = asmUsername,
                        onValueChange = { asmUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = asmMobile,
                        onValueChange = { asmMobile = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (asmName.isNotBlank() && asmUsername.isNotBlank()) {
                            onCreateAsm(asmName, asmUsername, asmMobile, reg.id)
                            showAddAsmDialogForRegion = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Create ASM")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAsmDialogForRegion = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add TL Dialog
    if (showAddTlDialogForAsm != null) {
        val asm = showAddTlDialogForAsm!!
        var tlName by remember { mutableStateOf("") }
        var tlUsername by remember { mutableStateOf("") }
        var tlMobile by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTlDialogForAsm = null },
            title = { Text("Add Team Leader under ASM: ${asm.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tlName,
                        onValueChange = { tlName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tlUsername,
                        onValueChange = { tlUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tlMobile,
                        onValueChange = { tlMobile = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tlName.isNotBlank() && tlUsername.isNotBlank()) {
                            onCreateTl(tlName, tlUsername, tlMobile, asm.regionId ?: 1L, asm.id)
                            showAddTlDialogForAsm = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Create TL")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTlDialogForAsm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add BDO Dialog
    if (showAddBdoDialogForTl != null) {
        val tl = showAddBdoDialogForTl!!
        var bdoName by remember { mutableStateOf("") }
        var bdoUsername by remember { mutableStateOf("") }
        var bdoMobile by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBdoDialogForTl = null },
            title = { Text("Add Field Officer (BDO) under TL: ${tl.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bdoName,
                        onValueChange = { bdoName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bdoUsername,
                        onValueChange = { bdoUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bdoMobile,
                        onValueChange = { bdoMobile = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bdoName.isNotBlank() && bdoUsername.isNotBlank()) {
                            onCreateBdo(bdoName, bdoUsername, bdoMobile, tl.regionId ?: 1L, tl.asmId ?: 0L, tl.id)
                            showAddBdoDialogForTl = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) {
                    Text("Create BDO")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddBdoDialogForTl = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RegionHierarchyCard(
    region: RegionEntity,
    asms: List<UserEntity>,
    allUsers: List<UserEntity>,
    merchantsCount: Int,
    onAddAsm: () -> Unit,
    onAddTl: (UserEntity) -> Unit,
    onAddBdo: (UserEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = region.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        )
                        Text(
                            text = "${asms.size} ASMs • $merchantsCount Merchants",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onAddAsm,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add ASM", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                if (asms.isEmpty()) {
                    Text(
                        text = "No ASMs assigned to this region yet.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        asms.forEach { asm ->
                            val asmTls = allUsers.filter { it.role == "TL" && it.asmId == asm.id }

                            AsmHierarchyItem(
                                asm = asm,
                                tls = asmTls,
                                allUsers = allUsers,
                                onAddTl = { onAddTl(asm) },
                                onAddBdo = onAddBdo
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AsmHierarchyItem(
    asm: UserEntity,
    tls: List<UserEntity>,
    allUsers: List<UserEntity>,
    onAddTl: () -> Unit,
    onAddBdo: (UserEntity) -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SupervisorAccount,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ASM: ${asm.name} (${asm.username})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        )
                        Text(
                            text = "Emp ID: ${asm.employeeId.ifBlank { "N/A" }} • Mobile: ${asm.mobile} • ${tls.size} Team Leaders",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAddTl,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Add TL", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (tls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tls.forEach { tl ->
                        val tlBdos = allUsers.filter { it.role == "BDO" && it.tlId == tl.id }

                        TlHierarchyItem(
                            tl = tl,
                            bdos = tlBdos,
                            onAddBdo = { onAddBdo(tl) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TlHierarchyItem(
    tl: UserEntity,
    bdos: List<UserEntity>,
    onAddBdo: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "TL: ${tl.name}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Navy900
                            )
                        )
                        Text(
                            text = "${bdos.size} Field Officers (BDOs)",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAddBdo,
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Add BDO", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (bdos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bdos.forEach { bdo ->
                        Surface(
                            color = EmeraldSubtle,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = bdo.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = EmeraldDark
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
