package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["role"]),
        Index(value = ["regionId"]),
        Index(value = ["asmId"]),
        Index(value = ["tlId"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: String, // "MASTER", "ADMIN", "ASM", "TL", "BDO"
    val name: String,
    val employeeId: String = "",
    val mobile: String,
    val email: String = "",
    val status: String = "Active", // "Active", "Inactive", "Suspended"
    val regionId: Long? = null,
    val asmId: Long? = null,
    val tlId: Long? = null,
    val managerId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

