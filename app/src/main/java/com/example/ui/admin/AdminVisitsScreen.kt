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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedCard
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
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.*
import com.example.util.DateUtils
import com.example.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminVisitsScreen(
    visits: List<VisitEntity>,
    merchants: List<MerchantEntity>,
    users: List<UserEntity>,
    onSelectVisit: (VisitEntity) -> Unit
) {
    var selectedGpsFilter by remember { mutableStateOf("All") } // "All", "VALID", "GPS MISMATCH"
    var selectedVerificationFilter by remember { mutableStateOf("All") } // "All", "Verified", "Needs Review", "Pending", "Rejected"
    var selectedBdoFilter by remember { mutableStateOf<Long?>(null) }
    var selectedVisitTypeFilter by remember { mutableStateOf("All") }

    val bdos = users.filter { it.role == "BDO" }

    val filteredVisits = visits.filter { v ->
        val matchesGps = selectedGpsFilter == "All" || v.gpsStatus.equals(selectedGpsFilter, ignoreCase = true)
        val matchesVerif = selectedVerificationFilter == "All" || v.verificationStatus.equals(selectedVerificationFilter, ignoreCase = true)
        val matchesBdo = selectedBdoFilter == null || v.bdoId == selectedBdoFilter
        val matchesType = selectedVisitTypeFilter == "All" || v.visitType.equals(selectedVisitTypeFilter, ignoreCase = true)

        matchesGps && matchesVerif && matchesBdo && matchesType
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_visits_screen"),
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
                    text = "Field Visits & Verification Log",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                )
                Text(
                    text = "${filteredVisits.size} of ${visits.size} Visits listed",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }
        }

        // Filters Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // GPS Filter
                    var gpsExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = gpsExpanded,
                        onExpandedChange = { gpsExpanded = it },
                        modifier = Modifier.width(160.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedGpsFilter,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("GPS Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gpsExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = gpsExpanded,
                            onDismissRequest = { gpsExpanded = false }
                        ) {
                            listOf("All", "VALID", "GPS MISMATCH").forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        selectedGpsFilter = opt
                                        gpsExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Verification Filter
                    var verifExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = verifExpanded,
                        onExpandedChange = { verifExpanded = it },
                        modifier = Modifier.width(160.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedVerificationFilter,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Verification") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = verifExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = verifExpanded,
                            onDismissRequest = { verifExpanded = false }
                        ) {
                            listOf("All", "Verified", "Needs Review", "Pending", "Rejected").forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        selectedVerificationFilter = opt
                                        verifExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // BDO Filter
                    var bdoExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = bdoExpanded,
                        onExpandedChange = { bdoExpanded = it },
                        modifier = Modifier.width(160.dp)
                    ) {
                        OutlinedTextField(
                            value = bdos.find { it.id == selectedBdoFilter }?.name ?: "All BDOs",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("BDO") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bdoExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = bdoExpanded,
                            onDismissRequest = { bdoExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All BDOs") },
                                onClick = {
                                    selectedBdoFilter = null
                                    bdoExpanded = false
                                }
                            )
                            bdos.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        selectedBdoFilter = b.id
                                        bdoExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visits List
        if (filteredVisits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No visits match the current filter selection.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SlateMuted)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredVisits, key = { it.id }) { visit ->
                    val merchant = merchants.find { it.merchantId == visit.merchantId }
                    val bdo = users.find { it.id == visit.bdoId }?.name ?: "BDO"

                    VisitLogCard(
                        visit = visit,
                        merchant = merchant,
                        bdoName = bdo,
                        onView = { onSelectVisit(visit) }
                    )
                }
            }
        }
    }
}

@Composable
fun VisitLogCard(
    visit: VisitEntity,
    merchant: MerchantEntity?,
    bdoName: String,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .testTag("visit_card_${visit.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Shop Name, Visit Type, Status badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = merchant?.shopName ?: visit.merchantId,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    )
                    Text(
                        text = "${visit.visitType} • BDO: $bdoName",
                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // GPS Status Badge
                    Surface(
                        color = if (visit.gpsStatus == "VALID") EmeraldSubtle else RedSubtle,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = visit.gpsStatus,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (visit.gpsStatus == "VALID") EmeraldDark else RedDanger
                            )
                        )
                    }

                    // Verification Status Badge
                    val (verifBg, verifColor) = when (visit.verificationStatus) {
                        "Verified" -> Pair(EmeraldSubtle, EmeraldDark)
                        "Rejected" -> Pair(RedSubtle, RedDanger)
                        "Needs Review" -> Pair(AmberSubtle, AmberWarning)
                        else -> Pair(PrimaryBlueSubtle, PrimaryBlue)
                    }
                    Surface(
                        color = verifBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = visit.verificationStatus,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = verifColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Distance & Location comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (visit.gpsStatus == "VALID") EmeraldDark else RedDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GPS Distance: ${GeoUtils.formatDistance(visit.distanceMeters)} (Accuracy: ${visit.gpsAccuracy.toInt()}m)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (visit.gpsStatus == "VALID") EmeraldDark else RedDanger
                        )
                    )
                }

                Text(
                    text = DateUtils.formatDateTime(visit.visitTime),
                    style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                )
            }

            if (visit.antiFraudFlags.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Flag: ${visit.antiFraudFlags}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = RedDanger
                    )
                )
            }

            if (visit.comments.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${visit.comments}\"",
                    style = MaterialTheme.typography.bodySmall.copy(color = Navy900),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (visit.photoPath.isNotBlank()) "Photo Attached" else "No Photo",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                    )
                }

                Button(
                    onClick = onView,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Audit Details", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
