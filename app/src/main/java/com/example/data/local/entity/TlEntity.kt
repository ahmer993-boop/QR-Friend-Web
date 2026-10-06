package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tls")
data class TlEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val regionId: Long,
    val status: String = "Active"
)
