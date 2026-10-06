package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TargetEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.AmberSubtle
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RedSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.DateUtils

@Composable
fun TargetsScreen(
    users: List<UserEntity>,
    targets: List<TargetEntity>,
    visits: List<VisitEntity>,
    onUpdateTarget: (TargetEntity) -> Unit
) {
    val bdos = users.filter { it.role == "BDO" }
    val today = DateUtils.getTodayDateString()

    var editingTargetForBdo by remember { mutableStateOf<UserEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("targets_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.TrackChanges, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Target Management & KPI Tracker",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    )
                }
                Text(
                    text = "Daily Goals vs Actual Performance per BDO (Target Achievement %)",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }

            Surface(
                color = PrimaryBlueSubtle,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Date: ${DateUtils.formatDateString(today)}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                )
            }
        }

        // BDO Targets List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bdos, key = { it.id }) { bdo ->
                val target = targets.find { it.bdoId == bdo.id && it.dateString == today } ?: TargetEntity(
                    bdoId = bdo.id,
                    dateString = today,
                    visitTarget = 10,
                    onboardingTarget = 5,
                    qrTarget = 5,
                    activationTarget = 4
                )

                val bdoTodayVisits = visits.filter { it.bdoId == bdo.id && it.visitDateString == today }
                val actualVisits = bdoTodayVisits.size
                val actualOnboarding = bdoTodayVisits.count { it.visitType.equals("New Onboarding", true) }
                val actualQr = bdoTodayVisits.count { it.qrDeployed }
                val actualActivation = bdoTodayVisits.count { it.visitType.equals("QR Activation", true) || it.merchantResponse.equals("Activated", true) }

                BdoTargetProgressCard(
                    bdo = bdo,
                    target = target,
                    actualVisits = actualVisits,
                    actualOnboarding = actualOnboarding,
                    actualQr = actualQr,
                    actualActivation = actualActivation,
                    onEdit = { editingTargetForBdo = bdo }
                )
            }
        }
    }

    // Edit Target Dialog
    if (editingTargetForBdo != null) {
        val bdo = editingTargetForBdo!!
        val currentTarget = targets.find { it.bdoId == bdo.id && it.dateString == today }

        var vTarget by remember { mutableStateOf((currentTarget?.visitTarget ?: 10).toString()) }
        var oTarget by remember { mutableStateOf((currentTarget?.onboardingTarget ?: 5).toString()) }
        var qTarget by remember { mutableStateOf((currentTarget?.qrTarget ?: 5).toString()) }
        var aTarget by remember { mutableStateOf((currentTarget?.activationTarget ?: 4).toString()) }

        AlertDialog(
            onDismissRequest = { editingTargetForBdo = null },
            title = { Text("Set Targets for ${bdo.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vTarget,
                        onValueChange = { vTarget = it },
                        label = { Text("Daily Visits Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = oTarget,
                        onValueChange = { oTarget = it },
                        label = { Text("Daily Onboarding Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = qTarget,
                        onValueChange = { qTarget = it },
                        label = { Text("Daily QR Deployment Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = aTarget,
                        onValueChange = { aTarget = it },
                        label = { Text("Daily QR Activation Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTarget = TargetEntity(
                            id = currentTarget?.id ?: 0,
                            bdoId = bdo.id,
                            dateString = today,
                            visitTarget = vTarget.toIntOrNull() ?: 10,
                            onboardingTarget = oTarget.toIntOrNull() ?: 5,
                            qrTarget = qTarget.toIntOrNull() ?: 5,
                            activationTarget = aTarget.toIntOrNull() ?: 4
                        )
                        onUpdateTarget(newTarget)
                        editingTargetForBdo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTargetForBdo = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BdoTargetProgressCard(
    bdo: UserEntity,
    target: TargetEntity,
    actualVisits: Int,
    actualOnboarding: Int,
    actualQr: Int,
    actualActivation: Int,
    onEdit: () -> Unit
) {
    val overallPct = (((actualVisits.toFloat() / target.visitTarget) +
            (actualOnboarding.toFloat() / target.onboardingTarget) +
            (actualQr.toFloat() / target.qrTarget) +
            (actualActivation.toFloat() / target.activationTarget)) / 4f * 100).toInt().coerceIn(0, 100)

    val (badgeBg, badgeText) = when {
        overallPct >= 80 -> Pair(EmeraldSubtle, EmeraldDark)
        overallPct >= 50 -> Pair(AmberSubtle, AmberWarning)
        else -> Pair(RedSubtle, RedDanger)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("target_card_${bdo.username}"),
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
                Column {
                    Text(
                        text = bdo.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )
                    Text(
                        text = "@${bdo.username} • Field Agent",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "$overallPct% Achieved",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = badgeText)
                        )
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Targets", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Items
            TargetProgressBar(label = "Field Visits", actual = actualVisits, target = target.visitTarget, color = PrimaryBlue)
            TargetProgressBar(label = "New Onboardings", actual = actualOnboarding, target = target.onboardingTarget, color = PrimaryBlueLight)
            TargetProgressBar(label = "QR Deployments", actual = actualQr, target = target.qrTarget, color = EmeraldDark)
            TargetProgressBar(label = "QR Activations", actual = actualActivation, target = target.activationTarget, color = EmeraldSuccess)
        }
    }
}

@Composable
fun TargetProgressBar(label: String, actual: Int, target: Int, color: Color) {
    val progress = (actual.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val pct = (progress * 100).toInt()

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = Navy900, fontWeight = FontWeight.Medium))
            Text(
                text = "$actual / $target ($pct%)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = color)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = SlateBorder
        )
    }
}
