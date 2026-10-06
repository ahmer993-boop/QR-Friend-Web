package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visits",
    indices = [
        Index(value = ["merchantId"]),
        Index(value = ["bdoId"]),
        Index(value = ["tlId"]),
        Index(value = ["asmId"]),
        Index(value = ["visitDateString"])
    ]
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitIdStr: String = "",
    val merchantId: String,
    val merchantName: String = "",
    val bdoId: Long,
    val bdoName: String = "",
    val tlId: Long = 0L,
    val asmId: Long = 0L,
    val visitType: String = "Follow-up Visit",
    val visitTime: Long = System.currentTimeMillis(),
    val visitDateString: String,
    val startTimeStr: String = "",
    val completionTimeStr: String = "",
    val latitude: Double,
    val longitude: Double,
    val gpsAccuracy: Float,
    val merchantLatitude: Double = 0.0,
    val merchantLongitude: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val gpsStatus: String = "VALID", // "VALID", "GPS MISMATCH"
    val visitStatus: String = "Completed", // "Started", "Completed"
    val visitRemarks: String = "",
    val photoPath: String = "",
    val qrPhotoPath: String = "",
    val qrDeployed: Boolean = false,
    val qrNotDeployedReason: String = "",
    val merchantStatus: String = "Active",
    val paymentDiscussion: Boolean = false,
    val merchantResponse: String = "Interested",
    val followUpRequired: Boolean = false,
    val followUpDate: String = "",
    val comments: String = "",
    val verificationStatus: String = "Pending", // "Pending", "Verified", "Rejected", "Needs Review"
    val verificationNotes: String = "",
    val antiFraudFlags: String = "",
    val transactionAmount: Double = 0.0,
    val isQualifiedDeployment: Boolean = false,
    val qrReceivedDate: String = "",
    val deploymentTatStatus: String = "Within TAT", // "Within TAT", "Due", "Overdue"
    val justificationReason: String = "",
    val justificationUser: String = "",
    val justificationDate: String = "",
    val isSynced: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

