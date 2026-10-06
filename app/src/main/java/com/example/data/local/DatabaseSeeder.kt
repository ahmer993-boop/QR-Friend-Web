package com.example.data.local

import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.UserEntity
import com.example.util.DateUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.firstOrNull

object DatabaseSeeder {
    suspend fun seedIfNeeded(db: AppDatabase) {
        val now = System.currentTimeMillis()
        val today = DateUtils.getTodayDateString()

        // 1. Ensure Base Operational Regions exist (standard corporate territories)
        val existingRegions = db.regionDao().getAllRegions().firstOrNull() ?: emptyList()
        if (existingRegions.isEmpty()) {
            db.regionDao().insertRegion(RegionEntity(name = "North Region"))
            db.regionDao().insertRegion(RegionEntity(name = "South Region"))
            db.regionDao().insertRegion(RegionEntity(name = "Central Region"))
        }

        // 2. Ensure Master Administrator account exists (ONLY if not present)
        val masterUser = db.userDao().findByUsername("master")
        if (masterUser == null) {
            val masterPasswordHash = SecurityUtils.hashPassword("Master@12345")
            val masterId = db.userDao().insertUser(
                UserEntity(
                    username = "master",
                    passwordHash = masterPasswordHash,
                    role = "MASTER",
                    name = "Master Administrator",
                    employeeId = "MST-001",
                    mobile = "+92 300 0000001",
                    email = "master@qrfriend.com",
                    status = "Active"
                )
            )

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = masterId,
                    userName = "Master Administrator",
                    action = "INITIALIZE_SYSTEM",
                    entityType = "USER",
                    entityId = "MST-001",
                    dateStr = today,
                    timeStr = DateUtils.formatTime(now),
                    recordAffected = "Master Administrator Account",
                    previousValue = "",
                    newValue = "Active",
                    metadata = "QR Friend System initialized. Master account provisioned."
                )
            )
        }

        // 4. Default Application Settings
        if (db.appSettingDao().getSetting("gps_threshold_meters") == null) {
            db.appSettingDao().setSetting(
                AppSettingEntity(
                    key = "gps_threshold_meters",
                    value = "100"
                )
            )
        }

        if (db.appSettingDao().getSetting("system_name") == null) {
            db.appSettingDao().setSetting(
                AppSettingEntity(
                    key = "system_name",
                    value = "QR Friend - Merchant Field Monitoring System"
                )
            )
        }
    }
}
