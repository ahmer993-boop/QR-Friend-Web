package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.QrRequestEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import java.io.File
import java.io.FileWriter

object ExcelExporter {

    private fun escapeCsv(value: Any?): String {
        if (value == null) return ""
        val str = value.toString().replace("\"", "\"\"")
        return if (str.contains(",") || str.contains("\n") || str.contains("\"")) {
            "\"$str\""
        } else {
            str
        }
    }

    /**
     * Exports Merchant Report to CSV
     */
    fun exportMerchantReport(
        context: Context,
        merchants: List<MerchantEntity>,
        users: List<UserEntity>
    ): File {
        val fileName = "Merchant_Report_${DateUtils.getTodayDateString().replace("-", "")}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = FileWriter(file)

        val userMap = users.associateBy { it.id }

        // Header
        writer.appendLine(
            listOf(
                "Merchant ID",
                "Merchant Name",
                "Shop/Business Name",
                "MSISDN / Mobile",
                "Alternate Mobile",
                "Region",
                "City",
                "Address",
                "ASM",
                "TL",
                "BDO",
                "Assignment Status",
                "QR Status",
                "QR ID",
                "Merchant Status"
            ).joinToString(",") { escapeCsv(it) }
        )

        merchants.forEach { m ->
            val bdo = userMap[m.bdoId]?.name ?: "Unassigned"
            val tl = userMap[m.tlId]?.name ?: "Unassigned"
            val asm = userMap[m.asmId]?.name ?: "Unassigned"
            val assignedStatus = if (m.bdoId != null && m.bdoId > 0) "Assigned" else "Unassigned"

            writer.appendLine(
                listOf(
                    m.merchantId,
                    m.merchantName,
                    m.shopName.ifBlank { m.businessName },
                    m.mobile,
                    m.alternateMobile,
                    m.city,
                    m.city,
                    m.address,
                    asm,
                    tl,
                    bdo,
                    assignedStatus,
                    m.qrStatus,
                    m.qrId,
                    m.merchantStatus
                ).joinToString(",") { escapeCsv(it) }
            )
        }

        writer.flush()
        writer.close()
        return file
    }

    /**
     * Exports Field Visit Report to CSV
     */
    fun exportVisitReport(
        context: Context,
        visits: List<VisitEntity>,
        merchants: List<MerchantEntity>,
        users: List<UserEntity>
    ): File {
        val fileName = "Field_Visit_Report_${DateUtils.getTodayDateString().replace("-", "")}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = FileWriter(file)

        val userMap = users.associateBy { it.id }
        val merchantMap = merchants.associateBy { it.merchantId }

        // Header
        writer.appendLine(
            listOf(
                "Visit ID",
                "Date",
                "Time",
                "BDO Name",
                "Team Leader",
                "ASM",
                "Merchant ID",
                "Shop Name",
                "Visit Type",
                "Visit Status",
                "Latitude",
                "Longitude",
                "GPS Accuracy (m)",
                "Distance (m)",
                "GPS Status",
                "QR Deployed",
                "Transaction Amount (Rs.)",
                "Deployment Qualified",
                "Merchant Response",
                "Verification Status",
                "Comments"
            ).joinToString(",") { escapeCsv(it) }
        )

        visits.forEach { v ->
            val bdo = userMap[v.bdoId]?.name ?: v.bdoName
            val tl = userMap[v.tlId]?.name ?: "N/A"
            val asm = userMap[v.asmId]?.name ?: "N/A"
            val m = merchantMap[v.merchantId]
            val shopName = m?.shopName ?: v.merchantName

            writer.appendLine(
                listOf(
                    v.visitIdStr.ifBlank { "VST-${v.id}" },
                    v.visitDateString,
                    v.completionTimeStr.ifBlank { DateUtils.formatTime(v.visitTime) },
                    bdo,
                    tl,
                    asm,
                    v.merchantId,
                    shopName,
                    v.visitType,
                    v.visitStatus,
                    v.latitude,
                    v.longitude,
                    v.gpsAccuracy,
                    v.distanceMeters.toInt(),
                    v.gpsStatus,
                    if (v.qrDeployed) "Yes" else "No",
                    v.transactionAmount,
                    if (v.isQualifiedDeployment) "QUALIFIED (>= 6000)" else "NOT QUALIFIED",
                    v.merchantResponse,
                    v.verificationStatus,
                    v.comments
                ).joinToString(",") { escapeCsv(it) }
            )
        }

        writer.flush()
        writer.close()
        return file
    }

    /**
     * Exports Alternate Days QR Deployment Monitoring Report
     */
    fun exportQrDeploymentReport(
        context: Context,
        merchants: List<MerchantEntity>,
        visits: List<VisitEntity>,
        users: List<UserEntity>
    ): File {
        val fileName = "QR_Deployment_Alternate_Days_Report_${DateUtils.getTodayDateString().replace("-", "")}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = FileWriter(file)

        val userMap = users.associateBy { it.id }
        val visitMap = visits.groupBy { it.merchantId }

        // Header
        writer.appendLine(
            listOf(
                "Merchant ID",
                "Shop Name",
                "City",
                "BDO",
                "TL",
                "ASM",
                "QR Status",
                "Deployment Rule Status",
                "Qualifying Transaction (Rs.)",
                "Date Received",
                "Deployment Date",
                "TAT (Days)",
                "TAT Status",
                "Non-Deployment Justification"
            ).joinToString(",") { escapeCsv(it) }
        )

        merchants.forEach { m ->
            val bdo = userMap[m.bdoId]?.name ?: "Unassigned"
            val tl = userMap[m.tlId]?.name ?: "Unassigned"
            val asm = userMap[m.asmId]?.name ?: "Unassigned"

            val mVisits = visitMap[m.merchantId] ?: emptyList()
            val qualifyingVisit = mVisits.find { it.isQualifiedDeployment || it.transactionAmount >= 6000.0 }
            val deploymentRuleStatus = if (qualifyingVisit != null) "QR DEPLOYED" else "QR AT HAND / NOT DEPLOYED"

            val isOverdue = m.qrStatus != "Deployed" && qualifyingVisit == null
            val tatStatus = if (qualifyingVisit != null) "Within TAT (Deployed)" else if (isOverdue) "Overdue" else "Due"

            writer.appendLine(
                listOf(
                    m.merchantId,
                    m.shopName.ifBlank { m.merchantName },
                    m.city,
                    bdo,
                    tl,
                    asm,
                    m.qrStatus,
                    deploymentRuleStatus,
                    qualifyingVisit?.transactionAmount ?: 0.0,
                    m.createdAt.let { DateUtils.formatDate(it) },
                    qualifyingVisit?.visitDateString ?: "Pending",
                    5,
                    tatStatus,
                    qualifyingVisit?.justificationReason ?: "N/A"
                ).joinToString(",") { escapeCsv(it) }
            )
        }

        writer.flush()
        writer.close()
        return file
    }

    /**
     * Exports QR Reprint & Printing Report
     */
    fun exportQrReprintReport(
        context: Context,
        requests: List<QrRequestEntity>
    ): File {
        val fileName = "QR_Reprint_Tracking_Report_${DateUtils.getTodayDateString().replace("-", "")}.csv"
        val file = File(context.cacheDir, fileName)
        val writer = FileWriter(file)

        // Header
        writer.appendLine(
            listOf(
                "Request ID",
                "Request Type",
                "Merchant ID",
                "Merchant Name",
                "City",
                "BDO",
                "TL",
                "ASM",
                "Request Date",
                "Request Reason",
                "Urgent Approval",
                "Vendor Submission Date",
                "Vendor Reference",
                "Printing Status",
                "Scanning Status",
                "Scanning Discrepancy",
                "Courier Name",
                "Tracking Number",
                "Dispatch Date",
                "Expected Delivery Date",
                "Actual Delivery Date",
                "Delivery Status",
                "Overall Status",
                "Escalated",
                "Justification"
            ).joinToString(",") { escapeCsv(it) }
        )

        requests.forEach { r ->
            writer.appendLine(
                listOf(
                    r.requestId,
                    r.requestType,
                    r.merchantId,
                    r.merchantName,
                    r.city,
                    r.bdoName,
                    r.tlName,
                    r.asmName,
                    "${r.requestDate} ${r.requestTime}",
                    r.reason,
                    if (r.isUrgent) "YES (Urgent: ${r.urgentApprovalNotes})" else "Standard",
                    r.vendorForwardedDate,
                    r.vendorReference,
                    r.status,
                    r.scanningStatus,
                    r.scanningDiscrepancy,
                    r.courierName,
                    r.trackingNumber,
                    r.dispatchDate,
                    r.expectedDeliveryDate,
                    r.actualDeliveryDate,
                    r.deliveryStatus,
                    r.status,
                    if (r.isEscalated) "YES (${r.escalationComments})" else "No",
                    r.nonDeploymentJustification
                ).joinToString(",") { escapeCsv(it) }
            )
        }

        writer.flush()
        writer.close()
        return file
    }

    fun shareOrOpenFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share $title")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
