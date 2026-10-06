package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchants",
    indices = [
        Index(value = ["merchantId"], unique = true),
        Index(value = ["regionId"]),
        Index(value = ["asmId"]),
        Index(value = ["tlId"]),
        Index(value = ["bdoId"])
    ]
)
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Basic Details
    val merchantId: String, // e.g. "M10025"
    val merchantName: String,
    val businessName: String = "",
    val shopName: String = businessName.ifBlank { merchantName },
    val cnicOrRegId: String = "",
    val mobile: String,
    val alternateMobile: String = "",
    val address: String,
    val city: String,

    // Organization Hierarchy
    val regionId: Long = 1,
    val regionName: String = "",
    val asmId: Long = 0,
    val asmName: String = "",
    val tlId: Long = 0,
    val tlCode: String = "",
    val tlName: String = "",
    val bdoId: Long = 0,
    val bdoCode: String = "",
    val bdoName: String = "",

    // GPS Location
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    // QR & Business Information
    val qrId: String = "",
    val qrStatus: String = "Not Deployed", // "Not Deployed", "Deployed", "Active", "Replacement Required"
    val merchantStatus: String = "New", // "New", "Onboarded", "Active", "Inactive", "Temporarily Closed", "Permanently Closed", "Refused"
    val merchantCategory: String = "General Store",
    val registrationDate: String = "",
    val onboardingDate: String = registrationDate,
    val lastVisitDate: String = "",
    val lastTransactionDate: String = "",
    val transactionAmount: Double = 0.0,
    val outstandingAmount: Double = 0.0,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

