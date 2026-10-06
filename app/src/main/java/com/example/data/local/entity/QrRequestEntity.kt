package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "qr_requests",
    indices = [
        Index(value = ["requestId"], unique = true),
        Index(value = ["merchantId"]),
        Index(value = ["asmId"]),
        Index(value = ["tlId"]),
        Index(value = ["bdoId"]),
        Index(value = ["status"]),
        Index(value = ["requestType"])
    ]
)
data class QrRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: String, // e.g. "REP-2026-000001" or "NEW-2026-000001"
    val requestType: String, // "EXTERNAL_REPRINT" or "NEW_PRINTING"
    val merchantId: String,
    val merchantName: String,
    val city: String,
    val regionId: Long,
    val regionName: String = "",
    val asmId: Long,
    val asmName: String = "",
    val tlId: Long,
    val tlName: String = "",
    val bdoId: Long,
    val bdoName: String = "",
    val requestDate: String,
    val requestTime: String,
    val requestTimestamp: Long = System.currentTimeMillis(),
    val reason: String = "", // e.g. "Damaged QR", "Lost QR", "New Onboarding", "Merchant Requested"
    val isUrgent: Boolean = false,
    val urgentApprovalNotes: String = "",
    val status: String = "PENDING_ASM", // "PENDING_ASM", "ASM_SUBMITTED", "FORWARDED_TO_VENDOR", "VENDOR_PRINTED", "SCANNED", "DISPATCHED", "DELIVERED", "DEPLOYED", "DISPUTED", "ESCALATED"
    val isOutstation: Boolean = false,
    val expectedCompletionDate: String = "",
    val vendorForwardedDate: String = "",
    val vendorReference: String = "",
    val coordinatorName: String = "",
    val scanningStatus: String = "Pending", // "Pending", "Completed", "Discrepancy"
    val scanningDiscrepancy: String = "", // Gap, Shortage, Missing QR, Printing Discrepancy
    val courierName: String = "", // "TCS", "Leopards", "M&P", "Direct Handover"
    val trackingNumber: String = "",
    val dispatchDate: String = "",
    val expectedDeliveryDate: String = "",
    val actualDeliveryDate: String = "",
    val deliveryStatus: String = "Pending", // "Pending", "In Transit", "Delivered", "Delayed"
    val isEscalated: Boolean = false,
    val escalationComments: String = "",
    val escalationTimestamp: Long = 0L,
    val disputeComments: String = "",
    val deploymentDate: String = "",
    val deploymentTatDays: Int = 5,
    val nonDeploymentJustification: String = "",
    val justificationUser: String = "",
    val justificationTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
