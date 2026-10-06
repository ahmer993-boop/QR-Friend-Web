package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val action: String,
    val entityType: String = "",
    val entityId: String = "",
    val dateStr: String = "",
    val timeStr: String = "",
    val recordAffected: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = ""
)

