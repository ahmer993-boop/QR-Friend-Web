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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandGreenDark
import com.example.ui.theme.BrandGreenSubtle
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy900
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RedSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.SecurityUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterUserManagementScreen(
    users: List<UserEntity>,
    regions: List<RegionEntity>,
    onToggleStatus: (userId: Long, newStatus: String) -> Unit,
    onReassign: (userId: Long, tlId: Long?, asmId: Long?, regionId: Long?) -> Unit,
    onResetPassword: (userId: Long, newPass: String) -> Unit,
    onEditUser: (UserEntity) -> Unit = {},
    onDeleteUser: (userId: Long) -> Unit = {},
    onCreateUser: (UserEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var userForEdit by remember { mutableStateOf<UserEntity?>(null) }
    var userForPasswordReset by remember { mutableStateOf<UserEntity?>(null) }
    var userForReassign by remember { mutableStateOf<UserEntity?>(null) }
    var userForDelete by remember { mutableStateOf<UserEntity?>(null) }
    var createdUserCredentials by remember { mutableStateOf<Triple<UserEntity, String, String>?>(null) }

    val roles = listOf("ALL", "MASTER", "ADMIN", "ASM", "TL", "BDO")
    val statuses = listOf("ALL", "Active", "Inactive")

    val visibleUsers = users.filter { it.status != "Deleted" }

    val filteredUsers = visibleUsers.filter { u ->
        val matchesRole = selectedRoleFilter == "ALL" || u.role.equals(selectedRoleFilter, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == "ALL" || u.status.equals(selectedStatusFilter, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
                u.name.contains(searchQuery, ignoreCase = true) ||
                u.username.contains(searchQuery, ignoreCase = true) ||
                u.employeeId.contains(searchQuery, ignoreCase = true) ||
                u.role.contains(searchQuery, ignoreCase = true)
        matchesRole && matchesStatus && matchesQuery
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card (Matching the uploaded Manage Accounts mock)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Manage Accounts",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenDark
                            )
                        )
                        Text(
                            text = "Full operational control over team access, roles, regional assignments, and login credentials.",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent Create Account Button - fully accessible on any screen size
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("create_user_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Create New Login ID / Account",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search & Filter Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, ID or role...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter dropdowns side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Role filter dropdown
                        var roleDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = roleDropdownExpanded,
                            onExpandedChange = { roleDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedRoleFilter == "ALL") "All Roles" else selectedRoleFilter,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = roleDropdownExpanded,
                                onDismissRequest = { roleDropdownExpanded = false }
                            ) {
                                roles.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(if (r == "ALL") "All Roles" else r) },
                                        onClick = {
                                            selectedRoleFilter = r
                                            roleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Status filter dropdown
                        var statusDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = statusDropdownExpanded,
                            onExpandedChange = { statusDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedStatusFilter == "ALL") "All Status" else selectedStatusFilter,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = statusDropdownExpanded,
                                onDismissRequest = { statusDropdownExpanded = false }
                            ) {
                                statuses.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(if (s == "ALL") "All Status" else s) },
                                        onClick = {
                                            selectedStatusFilter = s
                                            statusDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // Account Count summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Active Accounts: ${filteredUsers.size}",
                style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted, fontWeight = FontWeight.SemiBold)
            )
        }

        // User Accounts List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("users_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredUsers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = SlateMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No team accounts found",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            )
                            Text(
                                text = "Tap '+ Create New Account' above to create real BDO, TL, ASM, or Admin credentials.",
                                style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredUsers, key = { it.id }) { user ->
                    UserCardItem(
                        user = user,
                        regions = regions,
                        users = users,
                        onEdit = { userForEdit = user },
                        onToggleStatus = {
                            val newStatus = if (user.status == "Active") "Inactive" else "Active"
                            onToggleStatus(user.id, newStatus)
                        },
                        onResetPassword = { userForPasswordReset = user },
                        onReassign = { userForReassign = user },
                        onDelete = { userForDelete = user }
                    )
                }
            }
        }
    }

    // Floating Action Button to quickly create an account
    ExtendedFloatingActionButton(
        onClick = { showCreateDialog = true },
        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
        text = { Text("Create Account / ID") },
        containerColor = BrandGreen,
        contentColor = Color.White,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(20.dp)
            .testTag("fab_create_user")
    )
}

    // CREATE ACCOUNT DIALOG
    if (showCreateDialog) {
        CreateUserDialog(
            regions = regions,
            users = users,
            onDismiss = { showCreateDialog = false },
            onCreate = { newUser, plainPassword ->
                onCreateUser(newUser)
                createdUserCredentials = Triple(newUser, plainPassword, newUser.role)
                showCreateDialog = false
            }
        )
    }

    // SUCCESSFUL ACCOUNT CREATION CREDENTIALS POPUP
    if (createdUserCredentials != null) {
        val (u, plainPass, r) = createdUserCredentials!!
        AlertDialog(
            onDismissRequest = { createdUserCredentials = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = "Login ID Created Successfully!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("You can immediately log in using these credentials:", style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted))
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("User ID (Login):", fontWeight = FontWeight.SemiBold)
                            Text(u.username, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Password:", fontWeight = FontWeight.SemiBold)
                            Text(plainPass, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Role:", fontWeight = FontWeight.SemiBold)
                            Text(r, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Full Name:", fontWeight = FontWeight.SemiBold)
                            Text(u.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { createdUserCredentials = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("OK, Understood")
                }
            }
        )
    }

    // EDIT ACCOUNT DIALOG
    if (userForEdit != null) {
        EditUserDialog(
            user = userForEdit!!,
            regions = regions,
            users = users,
            onDismiss = { userForEdit = null },
            onSave = { updatedUser ->
                onEditUser(updatedUser)
                userForEdit = null
            }
        )
    }

    // RESET PASSWORD DIALOG
    if (userForPasswordReset != null) {
        ResetPasswordDialog(
            targetUser = userForPasswordReset!!,
            onDismiss = { userForPasswordReset = null },
            onConfirmReset = { newPass ->
                onResetPassword(userForPasswordReset!!.id, newPass)
                userForPasswordReset = null
            }
        )
    }

    // REASSIGN HIERARCHY DIALOG
    if (userForReassign != null) {
        ReassignHierarchyDialog(
            targetUser = userForReassign!!,
            regions = regions,
            users = users,
            onDismiss = { userForReassign = null },
            onConfirmReassign = { rId, asmId, tlId ->
                onReassign(userForReassign!!.id, tlId, asmId, rId)
                userForReassign = null
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (userForDelete != null) {
        AlertDialog(
            onDismissRequest = { userForDelete = null },
            title = {
                Text(
                    text = "Deactivate & Remove Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RedDanger)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to deactivate and remove ${userForDelete!!.name} (${userForDelete!!.username})?\n\nThis will revoke login access immediately. All historical field visits, QR deployment records, and merchant assignments will remain safely preserved in the database for audit integrity.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(userForDelete!!.id)
                        userForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("Deactivate & Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userForDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserCardItem(
    user: UserEntity,
    regions: List<RegionEntity>,
    users: List<UserEntity>,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onResetPassword: () -> Unit,
    onReassign: () -> Unit,
    onDelete: () -> Unit
) {
    val roleColor = when (user.role) {
        "MASTER" -> Color(0xFF7C3AED)
        "ADMIN" -> Color(0xFF1E40AF)
        "ASM" -> Color(0xFF0284C7)
        "TL" -> Color(0xFFD97706)
        else -> BrandGreen
    }

    val regionName = regions.find { it.id == user.regionId }?.name ?: "All Regions"
    val asmName = users.find { it.id == user.asmId }?.name
    val tlName = users.find { it.id == user.tlId }?.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (user.role) {
                                "MASTER" -> Icons.Default.Shield
                                "ADMIN" -> Icons.Default.AdminPanelSettings
                                "ASM" -> Icons.Default.SupervisorAccount
                                "TL" -> Icons.Default.Groups
                                else -> Icons.Default.Badge
                            },
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = roleColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = user.role,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = roleColor
                                    )
                                )
                            }
                        }
                        Text(
                            text = "User ID: ${user.username} • Code: ${user.employeeId.ifBlank { "N/A" }} • Phone: ${user.mobile.ifBlank { "N/A" }}",
                            style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                        )
                    }
                }

                // Status Badge (Active / Inactive)
                Surface(
                    color = if (user.status == "Active") EmeraldSubtle else RedSubtle,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = user.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (user.status == "Active") EmeraldDark else RedDanger
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hierarchy Breadcrumb
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hierarchy: $regionName",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                )
                if (asmName != null) {
                    Text(
                        text = " → ASM: $asmName",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                    )
                }
                if (tlName != null) {
                    Text(
                        text = " → TL: $tlName",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateBorder)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row (Edit, Reset Password, Reassign, Activate/Deactivate, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.role != "MASTER") {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Account",
                            tint = BrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onResetPassword,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = "Reset Password",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onReassign,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Reassign Hierarchy",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedButton(
                        onClick = onToggleStatus,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (user.status == "Active") RedDanger else EmeraldDark
                        )
                    ) {
                        Text(
                            text = if (user.status == "Active") "Deactivate" else "Activate",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete User",
                            tint = RedDanger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Text(
                        text = "System Owner • Protected Master Account",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

// CREATE USER DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserDialog(
    regions: List<RegionEntity>,
    users: List<UserEntity>,
    onDismiss: () -> Unit,
    onCreate: (UserEntity, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("password123") }
    var confirmPassword by remember { mutableStateOf("password123") }
    var passwordVisible by remember { mutableStateOf(true) }
    var role by remember { mutableStateOf("BDO") }
    var employeeCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var selectedRegionId by remember { mutableStateOf<Long?>(regions.firstOrNull()?.id) }
    var selectedAsmId by remember { mutableStateOf<Long?>(null) }
    var selectedTlId by remember { mutableStateOf<Long?>(null) }
    var isActive by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val roleOptions = listOf("BDO", "TL", "ASM", "ADMIN")
    val asmsInRegion = users.filter { it.role == "ASM" && (selectedRegionId == null || it.regionId == selectedRegionId) }
    val tlsForAsm = users.filter { it.role == "TL" && (selectedAsmId == null || it.asmId == selectedAsmId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New Team Account",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrandGreenDark
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = RedSubtle,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RedDanger, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = RedDanger, fontWeight = FontWeight.Medium))
                        }
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        errorMessage = null
                        if (username.isBlank() && it.isNotBlank()) {
                            val clean = it.trim().lowercase().replace("\\s+".toRegex(), "_")
                            username = "${role.lowercase()}_$clean"
                        }
                    },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // User ID / Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text("User ID / Login Username *") },
                    placeholder = { Text("e.g. bdo_bilal, tl_hassan") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Selector
                var roleDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = roleDropdownExpanded,
                    onExpandedChange = { roleDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false }
                    ) {
                        roleOptions.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    if (name.isNotBlank()) {
                                        val clean = name.trim().lowercase().replace("\\s+".toRegex(), "_")
                                        username = "${r.lowercase()}_$clean"
                                    }
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quick Password Fill
                OutlinedButton(
                    onClick = {
                        password = "password123"
                        confirmPassword = "password123"
                        passwordVisible = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quick Default Password: password123", style = MaterialTheme.typography.labelMedium)
                }

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password *") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = SlateMuted
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm Password *") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Employee Code & Phone
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = employeeCode,
                        onValueChange = { employeeCode = it },
                        label = { Text("Employee Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("03001234567") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Region selector
                var regionDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = regionDropdownExpanded,
                    onExpandedChange = { regionDropdownExpanded = it }
                ) {
                    val regionText = regions.find { it.id == selectedRegionId }?.name ?: "Select Region"
                    OutlinedTextField(
                        value = regionText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = regionDropdownExpanded,
                        onDismissRequest = { regionDropdownExpanded = false }
                    ) {
                        regions.forEach { reg ->
                            DropdownMenuItem(
                                text = { Text(reg.name) },
                                onClick = {
                                    selectedRegionId = reg.id
                                    selectedAsmId = null
                                    selectedTlId = null
                                    regionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // If BDO or TL, show ASM selector
                if (role == "BDO" || role == "TL") {
                    var asmDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = asmDropdownExpanded,
                        onExpandedChange = { asmDropdownExpanded = it }
                    ) {
                        val asmText = users.find { it.id == selectedAsmId }?.name ?: "Select Reporting ASM"
                        OutlinedTextField(
                            value = asmText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reporting ASM") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = asmDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = asmDropdownExpanded,
                            onDismissRequest = { asmDropdownExpanded = false }
                        ) {
                            asmsInRegion.forEach { asm ->
                                DropdownMenuItem(
                                    text = { Text(asm.name) },
                                    onClick = {
                                        selectedAsmId = asm.id
                                        selectedTlId = null
                                        asmDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // If BDO, show TL selector
                if (role == "BDO") {
                    var tlDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = tlDropdownExpanded,
                        onExpandedChange = { tlDropdownExpanded = it }
                    ) {
                        val tlText = users.find { it.id == selectedTlId }?.name ?: "Select Reporting TL"
                        OutlinedTextField(
                            value = tlText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reporting Team Leader (TL)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tlDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = tlDropdownExpanded,
                            onDismissRequest = { tlDropdownExpanded = false }
                        ) {
                            tlsForAsm.forEach { tl ->
                                DropdownMenuItem(
                                    text = { Text(tl.name) },
                                    onClick = {
                                        selectedTlId = tl.id
                                        tlDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Status Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account Status: ${if (isActive) "Active" else "Inactive"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandGreen, checkedTrackColor = BrandGreenSubtle)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanUsername = username.trim().lowercase()
                    if (name.isBlank()) {
                        errorMessage = "Full Name is required"
                        return@Button
                    }
                    if (cleanUsername.isBlank()) {
                        errorMessage = "User ID / Username is required"
                        return@Button
                    }
                    if (users.any { it.username.equals(cleanUsername, ignoreCase = true) }) {
                        errorMessage = "User ID '$cleanUsername' is already taken. Please choose a unique ID."
                        return@Button
                    }
                    if (password.length < 4) {
                        errorMessage = "Password must be at least 4 characters"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }

                    val passwordHash = SecurityUtils.hashPassword(password)
                    val cleanPhone = if (phone.trim().isBlank()) "03000000000" else phone.trim()
                    val newUser = UserEntity(
                        username = cleanUsername,
                        passwordHash = passwordHash,
                        role = role,
                        name = name.trim(),
                        employeeId = employeeCode.trim(),
                        mobile = cleanPhone,
                        email = email.trim(),
                        regionId = selectedRegionId,
                        asmId = selectedAsmId,
                        tlId = selectedTlId,
                        status = if (isActive) "Active" else "Inactive"
                    )
                    onCreate(newUser, password)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("Save Account")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// EDIT USER DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDialog(
    user: UserEntity,
    regions: List<RegionEntity>,
    users: List<UserEntity>,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var role by remember { mutableStateOf(user.role) }
    var employeeCode by remember { mutableStateOf(user.employeeId) }
    var phone by remember { mutableStateOf(user.mobile) }
    var email by remember { mutableStateOf(user.email) }
    var selectedRegionId by remember { mutableStateOf(user.regionId) }
    var selectedAsmId by remember { mutableStateOf(user.asmId) }
    var selectedTlId by remember { mutableStateOf(user.tlId) }
    var isActive by remember { mutableStateOf(user.status == "Active") }

    val roleOptions = listOf("BDO", "TL", "ASM", "ADMIN")
    val asmsInRegion = users.filter { it.role == "ASM" && (selectedRegionId == null || it.regionId == selectedRegionId) }
    val tlsForAsm = users.filter { it.role == "TL" && (selectedAsmId == null || it.asmId == selectedAsmId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Account (${user.username})",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrandGreenDark
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = user.username,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("User ID (Read-only)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                var roleDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = roleDropdownExpanded,
                    onExpandedChange = { roleDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false }
                    ) {
                        roleOptions.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = employeeCode,
                        onValueChange = { employeeCode = it },
                        label = { Text("Employee / BDO Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Region selector
                var regionDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = regionDropdownExpanded,
                    onExpandedChange = { regionDropdownExpanded = it }
                ) {
                    val regionText = regions.find { it.id == selectedRegionId }?.name ?: "All Regions"
                    OutlinedTextField(
                        value = regionText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = regionDropdownExpanded,
                        onDismissRequest = { regionDropdownExpanded = false }
                    ) {
                        regions.forEach { reg ->
                            DropdownMenuItem(
                                text = { Text(reg.name) },
                                onClick = {
                                    selectedRegionId = reg.id
                                    selectedAsmId = null
                                    selectedTlId = null
                                    regionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (role == "BDO" || role == "TL") {
                    var asmDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = asmDropdownExpanded,
                        onExpandedChange = { asmDropdownExpanded = it }
                    ) {
                        val asmText = users.find { it.id == selectedAsmId }?.name ?: "Select Reporting ASM"
                        OutlinedTextField(
                            value = asmText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reporting ASM") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = asmDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = asmDropdownExpanded,
                            onDismissRequest = { asmDropdownExpanded = false }
                        ) {
                            asmsInRegion.forEach { asm ->
                                DropdownMenuItem(
                                    text = { Text(asm.name) },
                                    onClick = {
                                        selectedAsmId = asm.id
                                        selectedTlId = null
                                        asmDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (role == "BDO") {
                    var tlDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = tlDropdownExpanded,
                        onExpandedChange = { tlDropdownExpanded = it }
                    ) {
                        val tlText = users.find { it.id == selectedTlId }?.name ?: "Select Reporting TL"
                        OutlinedTextField(
                            value = tlText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reporting Team Leader (TL)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tlDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = tlDropdownExpanded,
                            onDismissRequest = { tlDropdownExpanded = false }
                        ) {
                            tlsForAsm.forEach { tl ->
                                DropdownMenuItem(
                                    text = { Text(tl.name) },
                                    onClick = {
                                        selectedTlId = tl.id
                                        tlDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account Status: ${if (isActive) "Active" else "Inactive"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandGreen, checkedTrackColor = BrandGreenSubtle)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        user.copy(
                            name = name.trim(),
                            role = role,
                            employeeId = employeeCode.trim(),
                            mobile = phone.trim(),
                            email = email.trim(),
                            regionId = selectedRegionId,
                            asmId = selectedAsmId,
                            tlId = selectedTlId,
                            status = if (isActive) "Active" else "Inactive"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// RESET PASSWORD DIALOG
@Composable
fun ResetPasswordDialog(
    targetUser: UserEntity,
    onDismiss: () -> Unit,
    onConfirmReset: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reset Password for ${targetUser.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrandGreenDark)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Set a secure new password for user ID '${targetUser.username}'.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )

                if (errorMsg != null) {
                    Text(text = errorMsg!!, style = MaterialTheme.typography.labelSmall.copy(color = RedDanger))
                }

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMsg = null },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMsg = null },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.length < 6) {
                        errorMsg = "Password must be at least 6 characters"
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMsg = "Passwords do not match"
                        return@Button
                    }
                    onConfirmReset(newPassword)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("Confirm Password Reset")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// REASSIGN HIERARCHY DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReassignHierarchyDialog(
    targetUser: UserEntity,
    regions: List<RegionEntity>,
    users: List<UserEntity>,
    onDismiss: () -> Unit,
    onConfirmReassign: (Long?, Long?, Long?) -> Unit
) {
    var selectedRegionId by remember { mutableStateOf(targetUser.regionId) }
    var selectedAsmId by remember { mutableStateOf(targetUser.asmId) }
    var selectedTlId by remember { mutableStateOf(targetUser.tlId) }

    val asmsInRegion = users.filter { it.role == "ASM" && (selectedRegionId == null || it.regionId == selectedRegionId) }
    val tlsForAsm = users.filter { it.role == "TL" && (selectedAsmId == null || it.asmId == selectedAsmId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reassign Hierarchy for ${targetUser.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrandGreenDark)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Region dropdown
                var rExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = rExp, onExpandedChange = { rExp = it }) {
                    OutlinedTextField(
                        value = regions.find { it.id == selectedRegionId }?.name ?: "All Regions",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rExp) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = rExp, onDismissRequest = { rExp = false }) {
                        regions.forEach { reg ->
                            DropdownMenuItem(text = { Text(reg.name) }, onClick = {
                                selectedRegionId = reg.id
                                selectedAsmId = null
                                selectedTlId = null
                                rExp = false
                            })
                        }
                    }
                }

                if (targetUser.role == "BDO" || targetUser.role == "TL") {
                    var asmExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = asmExp, onExpandedChange = { asmExp = it }) {
                        OutlinedTextField(
                            value = users.find { it.id == selectedAsmId }?.name ?: "Select ASM",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reporting ASM") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = asmExp) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = asmExp, onDismissRequest = { asmExp = false }) {
                            asmsInRegion.forEach { asm ->
                                DropdownMenuItem(text = { Text(asm.name) }, onClick = {
                                    selectedAsmId = asm.id
                                    selectedTlId = null
                                    asmExp = false
                                })
                            }
                        }
                    }
                }

                if (targetUser.role == "BDO") {
                    var tlExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = tlExp, onExpandedChange = { tlExp = it }) {
                        OutlinedTextField(
                            value = users.find { it.id == selectedTlId }?.name ?: "Select TL",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reporting TL") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tlExp) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = tlExp, onDismissRequest = { tlExp = false }) {
                            tlsForAsm.forEach { tl ->
                                DropdownMenuItem(text = { Text(tl.name) }, onClick = {
                                    selectedTlId = tl.id
                                    tlExp = false
                                })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReassign(selectedRegionId, selectedAsmId, selectedTlId) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                Text("Confirm Reassignment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
