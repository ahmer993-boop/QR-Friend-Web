package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "targets",
    indices = [Index(value = ["bdoId", "dateString"], unique = true)]
)
data class TargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bdoId: Long,
    val dateString: String,
    val visitTarget: Int = 10,
    val onboardingTarget: Int = 5,
    val qrTarget: Int = 5,
    val activationTarget: Int = 4
)
