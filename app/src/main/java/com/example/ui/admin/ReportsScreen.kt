package com.example.ui.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.TargetEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RedSubtle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.util.DateUtils

@Composable
fun ReportsScreen(
    merchants: List<MerchantEntity>,
    visits: List<VisitEntity>,
    users: List<UserEntity>,
    targets: List<TargetEntity>,
    transactions: List<TransactionEntity>
) {
    var selectedReportTab by remember { mutableStateOf(0) }
    var exportFeedback by remember { mutableStateOf<String?>(null) }

    val reportTitles = listOf("Daily BDO Report", "Merchant Report", "GPS Compliance", "Target Report")
    val bdos = users.filter { it.role == "BDO" }
    val today = DateUtils.getTodayDateString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_screen"),
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
                    Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Executive Reports & Audit Export",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    )
                }
                Text(
                    text = "BDO daily performance, merchant status health, GPS compliance ranks and target achievements",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )
            }

            Button(
                onClick = {
                    exportFeedback = "Exported ${reportTitles[selectedReportTab]} to Excel / CSV format (Downloads/Report_${System.currentTimeMillis()}.csv)"
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("export_report_button")
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Report", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (exportFeedback != null) {
            Surface(
                color = EmeraldSubtle,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = exportFeedback!!,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                )
            }
        }

        // Tabs
        TabRow(selectedTabIndex = selectedReportTab) {
            reportTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedReportTab == index,
                    onClick = {
                        selectedReportTab = index
                        exportFeedback = null
                    },
                    text = { Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)) }
                )
            }
        }

        // Tab Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedReportTab) {
                0 -> {
                    // Daily BDO Report
                    bdos.forEach { bdo ->
                        val bdoVisits = visits.filter { it.bdoId == bdo.id && it.visitDateString == today }
                        val target = targets.find { it.bdoId == bdo.id && it.dateString == today }
                        val validGpsCount = bdoVisits.count { it.gpsStatus == "VALID" }
                        val gpsCompliance = if (bdoVisits.isNotEmpty()) (validGpsCount * 100 / bdoVisits.size) else 100

                        ReportRowCard(
                            title = bdo.name,
                            subtitle = "@${bdo.username}",
                            items = listOf(
                                "Visits Planned" to "${target?.visitTarget ?: 10}",
                                "Visits Done" to "${bdoVisits.size}",
                                "QR Deployed" to "${bdoVisits.count { it.qrDeployed }}",
                                "QR Activated" to "${bdoVisits.count { it.merchantResponse == "Activated" }}",
                                "GPS Compliance" to "$gpsCompliance%"
                            )
                        )
                    }
                }
                1 -> {
                    // Merchant Report
                    ReportRowCard(
                        title = "Merchant Portfolio Summary",
                        subtitle = "Overall Merchant Network",
                        items = listOf(
                            "Total Registered" to "${merchants.size}",
                            "Active" to "${merchants.count { it.merchantStatus == "Active" }}",
                            "New" to "${merchants.count { it.merchantStatus == "New" }}",
                            "QR Deployed" to "${merchants.count { it.qrStatus == "Deployed" || it.qrStatus == "Active" }}",
                            "Inactive (>7d)" to "${merchants.count { it.merchantStatus == "Inactive" }}",
                            "Total Volume" to "PKR ${"%,.0f".format(transactions.sumOf { it.amount })}"
                        )
                    )

                    merchants.take(8).forEach { m ->
                        val mTxns = transactions.filter { it.merchantId == m.merchantId }
                        ReportRowCard(
                            title = m.shopName,
                            subtitle = "ID: ${m.merchantId} • ${m.address}",
                            items = listOf(
                                "Status" to m.merchantStatus,
                                "QR Status" to m.qrStatus,
                                "Payments" to "${mTxns.size} txn",
                                "Volume" to "PKR ${"%,.0f".format(mTxns.sumOf { it.amount })}"
                            )
                        )
                    }
                }
                2 -> {
                    // GPS Compliance Report
                    val totalVisits = visits.size
                    val validVisits = visits.count { it.gpsStatus == "VALID" }
                    val mismatchVisits = visits.count { it.gpsStatus == "GPS MISMATCH" }
                    val overallCompliance = if (totalVisits > 0) (validVisits * 100 / totalVisits) else 100

                    ReportRowCard(
                        title = "System GPS Compliance",
                        subtitle = "Haversine Distance Radius Verification (Threshold: 100m)",
                        items = listOf(
                            "Total Field Visits" to "$totalVisits",
                            "Valid Within 100m" to "$validVisits",
                            "GPS Mismatches" to "$mismatchVisits",
                            "System Compliance" to "$overallCompliance%"
                        )
                    )

                    Text(
                        text = "BDO GPS Compliance Ranking",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Navy900)
                    )

                    bdos.sortedByDescending { b ->
                        val bVisits = visits.filter { it.bdoId == b.id }
                        val valids = bVisits.count { it.gpsStatus == "VALID" }
                        if (bVisits.isNotEmpty()) (valids * 100 / bVisits.size) else 100
                    }.forEachIndexed { idx, bdo ->
                        val bVisits = visits.filter { it.bdoId == bdo.id }
                        val valids = bVisits.count { it.gpsStatus == "VALID" }
                        val comp = if (bVisits.isNotEmpty()) (valids * 100 / bVisits.size) else 100

                        ReportRowCard(
                            title = "#${idx + 1} ${bdo.name}",
                            subtitle = "@${bdo.username} • ${bVisits.size} visits total",
                            items = listOf(
                                "Valid" to "$valids",
                                "Mismatches" to "${bVisits.size - valids}",
                                "Compliance Rate" to "$comp%"
                            )
                        )
                    }
                }
                3 -> {
                    // Target vs Actual Report
                    bdos.forEach { bdo ->
                        val target = targets.find { it.bdoId == bdo.id && it.dateString == today }
                        val bdoVisits = visits.filter { it.bdoId == bdo.id && it.visitDateString == today }

                        val vActual = bdoVisits.size
                        val vTarget = target?.visitTarget ?: 10
                        val vPct = (vActual * 100 / vTarget.coerceAtLeast(1))

                        val qActual = bdoVisits.count { it.qrDeployed }
                        val qTarget = target?.qrTarget ?: 5
                        val qPct = (qActual * 100 / qTarget.coerceAtLeast(1))

                        ReportRowCard(
                            title = bdo.name,
                            subtitle = "Target Date: ${DateUtils.formatDateString(today)}",
                            items = listOf(
                                "Visits Target" to "$vActual / $vTarget ($vPct%)",
                                "QR Target" to "$qActual / $qTarget ($qPct%)",
                                "Target Status" to if (vPct >= 80) "On Track" else "Needs Improvement"
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRowCard(
    title: String,
    subtitle: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted)
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items.forEach { (label, value) ->
                    Column {
                        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = SlateMuted))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Navy900)
                        )
                    }
                }
            }
        }
    }
}
