package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["merchantId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantId: String,
    val transactionId: String,
    val amount: Double,
    val transactionDate: Long = System.currentTimeMillis(),
    val isQrPayment: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
