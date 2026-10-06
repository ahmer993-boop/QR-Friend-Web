package com.example.ui.bdo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TargetEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.admin.TargetProgressBar
import com.example.ui.theme.*
import com.example.util.DateUtils

@Composable
fun BdoTargetsScreen(
    bdoUser: UserEntity,
    target: TargetEntity?,
    visits: List<VisitEntity>
) {
    val today = DateUtils.getTodayDateString()
    val todayVisits = visits.filter { it.bdoId == bdoUser.id && it.visitDateString == today }

    val actualVisits = todayVisits.size
    val visitTarget = target?.visitTarget ?: 10

    val actualOnboardings = todayVisits.count { it.visitType.equals("New Onboarding", true) }
    val onboardingTarget = target?.onboardingTarget ?: 5

    val actualQr = todayVisits.count { it.qrDeployed }
    val qrTarget = target?.qrTarget ?: 5

    val actualActivation = todayVisits.count { it.visitType.equals("QR Activation", true) || it.merchantResponse.equals("Activated", true) }
    val activationTarget = target?.activationTarget ?: 4

    val overallPct = (((actualVisits.toFloat() / visitTarget) +
            (actualOnboardings.toFloat() / onboardingTarget) +
            (actualQr.toFloat() / qrTarget) +
            (actualActivation.toFloat() / activationTarget)) / 4f * 100).toInt().coerceIn(0, 100)

    val (badgeBg, badgeText, badgeBorder) = when {
        overallPct >= 80 -> Triple(SuccessBg, SuccessText, Color(0xFF86EFAC))
        overallPct >= 50 -> Triple(WarningBg, WarningOrange, Color(0xFFFDE68A))
        else -> Triple(DangerBg, DangerText, Color(0xFFFCA5A5))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(16.dp)
            .testTag("bdo_targets_screen"),
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
                    text = "My Performance Targets",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextHeadings
                    )
                )
                Text(
                    text = "Target Date: ${DateUtils.formatDateString(today)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, badgeBorder)
            ) {
                Text(
                    text = "$overallPct% Achieved",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = badgeText)
                )
            }
        }

        // Summary Achievement Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, BorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daily Goals Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TargetProgressBar(
                    label = "Merchant Field Visits",
                    actual = actualVisits,
                    target = visitTarget,
                    color = BrandGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                TargetProgressBar(
                    label = "New Merchant Onboardings",
                    actual = actualOnboardings,
                    target = onboardingTarget,
                    color = BrandGreenDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                TargetProgressBar(
                    label = "QR Standee Deployments",
                    actual = actualQr,
                    target = qrTarget,
                    color = BrandGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                TargetProgressBar(
                    label = "First QR Transaction Activations",
                    actual = actualActivation,
                    target = activationTarget,
                    color = SuccessGreen
                )
            }
        }

        // Motivation / Guidance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BrandGreenLight),
            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Field Force Incentive Tips",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrandGreenDark)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Maintain 100% GPS compliance by completing visits directly inside the merchant's shop.\n" +
                            "• Ensure QR standees are visible on counter cash desks.\n" +
                            "• Conduct live payment demo to activate merchant within 48 hours.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                )
            }
        }
    }
}
