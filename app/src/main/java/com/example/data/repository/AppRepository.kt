package com.example.data.repository

import com.example.data.cloud.CloudSyncManager
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseSeeder
import com.example.data.local.entity.AppSettingEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.QrRequestEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.TargetEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.util.DateUtils
import com.example.util.GeoUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val db: AppDatabase) {

    val database: AppDatabase get() = db

    suspend fun insertAuditLog(
        user: UserEntity,
        action: String,
        entityType: String,
        entityId: String,
        recordAffected: String,
        prevVal: String = "",
        newVal: String = "",
        metadata: String = ""
    ) {
        val now = System.currentTimeMillis()
        val log = AuditLogEntity(
            userId = user.id,
            userName = user.name,
            action = action,
            entityType = entityType,
            entityId = entityId,
            dateStr = DateUtils.getTodayDateString(),
            timeStr = DateUtils.formatTime(now),
            timestamp = now,
            recordAffected = recordAffected,
            previousValue = prevVal,
            newValue = newVal,
            metadata = metadata
        )
        db.auditLogDao().insertLog(log)
        // Replicate immutable audit event to central cloud database
        CloudSyncManager.recordAuditLogToCloud(log)
    }

    suspend fun seedDatabaseIfNeeded() {
        DatabaseSeeder.seedIfNeeded(db)
    }

    val allMerchants: Flow<List<MerchantEntity>> = db.merchantDao().getAllMerchants()
    val allVisits: Flow<List<VisitEntity>> = db.visitDao().getAllVisits()
    val verificationQueue: Flow<List<VisitEntity>> = db.visitDao().getVerificationQueue()
    val allRegions: Flow<List<RegionEntity>> = db.regionDao().getAllRegions()
    val allTls: Flow<List<TlEntity>> = db.tlDao().getAllTls()
    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val allTargets: Flow<List<TargetEntity>> = db.targetDao().getAllTargets()
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val allAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllAuditLogs()
    val unsyncedVisits: Flow<List<VisitEntity>> = db.visitDao().getUnsyncedVisits()
    val allQrRequests: Flow<List<QrRequestEntity>> = db.qrRequestDao().getAllQrRequests()

    suspend fun login(username: String, plainPassword: String): Pair<UserEntity?, String?> {
        val cleanUsername = username.trim().lowercase()
        val cleanPassword = plainPassword.trim()

        DatabaseSeeder.seedIfNeeded(db)

        var user = db.userDao().findByUsername(cleanUsername)
        if (user == null) {
            // Check real-time cloud database (e.g. BDO account created on Master device logging into new phone)
            val cloudUser = CloudSyncManager.fetchUserFromCloud(cleanUsername)
            if (cloudUser != null) {
                db.userDao().insertUser(cloudUser)
                user = cloudUser
            }
        }

        if (user == null) {
            return Pair(null, "Invalid username or password.")
        }

        if (user.status.equals("Deleted", ignoreCase = true)) {
            return Pair(null, "This user account has been deleted.")
        }
        if (user.status.equals("Suspended", ignoreCase = true) || user.status.equals("Inactive", ignoreCase = true)) {
            return Pair(null, "Account is inactive or suspended. Please contact administrator.")
        }

        val isPasswordValid = SecurityUtils.verifyPassword(cleanPassword, user.passwordHash)
        if (!isPasswordValid) {
            return Pair(null, "Invalid username or password.")
        }

        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = user.id,
                userName = user.name,
                action = "LOGIN_SUCCESS",
                entityType = "USER",
                entityId = user.id.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "User: ${user.username} (${user.role})",
                metadata = "User logged in with role ${user.role}"
            )
        )
        return Pair(user, null)
    }

    // Role-filtered streams
    fun getMerchantsForBdo(bdoId: Long): Flow<List<MerchantEntity>> =
        db.merchantDao().getMerchantsByBdo(bdoId)

    fun getVisitsForBdo(bdoId: Long): Flow<List<VisitEntity>> =
        db.visitDao().getVisitsByBdo(bdoId)

    fun getTargetsForBdo(bdoId: Long): Flow<List<TargetEntity>> =
        db.targetDao().getTargetsByBdo(bdoId)

    // TL streams
    fun getBdosForTl(tlId: Long): Flow<List<UserEntity>> =
        db.userDao().getBdosByTl(tlId)

    fun getMerchantsForTl(tlId: Long): Flow<List<MerchantEntity>> =
        db.merchantDao().getMerchantsByTl(tlId)

    fun getVisitsForTl(tlId: Long): Flow<List<VisitEntity>> =
        db.visitDao().getVisitsByTl(tlId)

    // ASM streams
    fun getTlsForAsm(asmId: Long): Flow<List<UserEntity>> =
        db.userDao().getTlsByAsm(asmId)

    fun getBdosForAsm(asmId: Long): Flow<List<UserEntity>> =
        db.userDao().getBdosByAsm(asmId)

    fun getMerchantsForAsm(asmId: Long): Flow<List<MerchantEntity>> =
        db.merchantDao().getMerchantsByAsm(asmId)

    fun getVisitsForAsm(asmId: Long): Flow<List<VisitEntity>> =
        db.visitDao().getVisitsByAsm(asmId)

    suspend fun getTodayTargetForBdo(bdoId: Long, dateString: String = DateUtils.getTodayDateString()): TargetEntity? {
        return db.targetDao().findTargetByBdoAndDate(bdoId, dateString)
    }

    suspend fun getGpsThresholdMeters(): Double {
        val raw = db.appSettingDao().getSettingValue("gps_threshold_meters") ?: "100"
        return raw.toDoubleOrNull() ?: 100.0
    }

    suspend fun setGpsThresholdMeters(meters: Double, adminUser: UserEntity) {
        db.appSettingDao().setSetting(AppSettingEntity("gps_threshold_meters", meters.toString()))
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = adminUser.id,
                userName = adminUser.name,
                action = "UPDATE_GPS_THRESHOLD",
                entityType = "SETTING",
                entityId = "gps_threshold_meters",
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(System.currentTimeMillis()),
                recordAffected = "GPS Threshold Setting",
                previousValue = "100m",
                newValue = "${meters}m",
                metadata = "Updated GPS threshold to ${meters}m"
            )
        )
    }

    suspend fun getMerchantById(merchantId: String): MerchantEntity? {
        return db.merchantDao().findMerchantByMerchantId(merchantId)
    }

    /**
     * Records a field visit with mandatory GPS, device coordinates, and QR deployment rule:
     * Deployed only when qualifying transaction >= Rs. 6,000 and physical QR handed over.
     */
    suspend fun submitVisit(
        merchant: MerchantEntity,
        bdoUser: UserEntity,
        visitType: String,
        visitLatitude: Double,
        visitLongitude: Double,
        gpsAccuracy: Float,
        photoPath: String,
        qrPhotoPath: String = "",
        qrDeployed: Boolean,
        qrNotDeployedReason: String,
        merchantStatus: String,
        paymentDiscussion: Boolean,
        merchantResponse: String,
        followUpRequired: Boolean,
        followUpDate: String,
        comments: String,
        transactionAmount: Double = 0.0,
        qrPhysicalReceived: Boolean = false,
        qrReceivedDate: String = "",
        justificationReason: String = "",
        isOnline: Boolean = true
    ): VisitEntity {
        val now = System.currentTimeMillis()
        val today = DateUtils.getTodayDateString()
        val threshold = getGpsThresholdMeters()

        val distanceMeters = GeoUtils.calculateHaversineDistance(
            merchant.latitude,
            merchant.longitude,
            visitLatitude,
            visitLongitude
        )

        val gpsStatus = GeoUtils.getGpsStatus(distanceMeters, threshold)

        val fraudFlags = mutableListOf<String>()
        if (gpsStatus == "GPS MISMATCH") {
            fraudFlags.add("GPS MISMATCH (${GeoUtils.formatDistance(distanceMeters)})")
        }
        if (gpsAccuracy > 40f) {
            fraudFlags.add("POOR GPS ACCURACY (${gpsAccuracy.toInt()}m)")
        }

        val verificationStatus = if (gpsStatus == "GPS MISMATCH" || fraudFlags.isNotEmpty()) {
            "Needs Review"
        } else {
            "Verified"
        }

        val visitIdStr = "VISIT-${DateUtils.getTodayDateString().replace("-", "")}-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}"

        // Rule: QR is DEPLOYED ONLY when transaction >= 6000 and QR handed over
        val isQualified = transactionAmount >= 6000.0 && qrPhysicalReceived
        val effectiveQrDeployed = isQualified || (qrDeployed && transactionAmount >= 6000.0)
        val deploymentTatStatus = if (isQualified) "Within TAT" else "Due"

        val visit = VisitEntity(
            visitIdStr = visitIdStr,
            merchantId = merchant.merchantId,
            merchantName = merchant.businessName.ifBlank { merchant.merchantName },
            bdoId = bdoUser.id,
            bdoName = bdoUser.name,
            tlId = bdoUser.tlId ?: 0L,
            asmId = bdoUser.asmId ?: 0L,
            visitType = visitType,
            visitTime = now,
            visitDateString = today,
            startTimeStr = DateUtils.formatTime(now - 20 * 60 * 1000),
            completionTimeStr = DateUtils.formatTime(now),
            latitude = visitLatitude,
            longitude = visitLongitude,
            gpsAccuracy = gpsAccuracy,
            merchantLatitude = merchant.latitude,
            merchantLongitude = merchant.longitude,
            distanceMeters = distanceMeters,
            gpsStatus = gpsStatus,
            visitStatus = "Completed",
            photoPath = photoPath,
            qrPhotoPath = qrPhotoPath,
            qrDeployed = effectiveQrDeployed,
            qrNotDeployedReason = if (!effectiveQrDeployed && qrNotDeployedReason.isBlank()) "No qualifying transaction (>= Rs. 6,000)" else qrNotDeployedReason,
            merchantStatus = merchantStatus,
            paymentDiscussion = paymentDiscussion,
            merchantResponse = merchantResponse,
            followUpRequired = followUpRequired,
            followUpDate = followUpDate,
            comments = comments,
            verificationStatus = verificationStatus,
            antiFraudFlags = fraudFlags.joinToString(", "),
            transactionAmount = transactionAmount,
            isQualifiedDeployment = isQualified,
            qrReceivedDate = qrReceivedDate,
            deploymentTatStatus = deploymentTatStatus,
            justificationReason = justificationReason,
            isSynced = isOnline
        )

        val visitId = db.visitDao().insertVisit(visit)
        val createdVisit = visit.copy(id = visitId)
        // Automatic real-time sync to cloud (or queued offline if disconnected)
        CloudSyncManager.recordVisitToCloud(createdVisit)

        // Update Merchant status and QR status according to Rule
        val newQrStatus = if (effectiveQrDeployed) "Deployed" else "At Hand"
        db.merchantDao().updateStatusAndQr(
            merchantId = merchant.merchantId,
            status = merchantStatus,
            qrStatus = newQrStatus,
            qrId = merchant.qrId
        )
        val updatedMerchant = merchant.copy(
            merchantStatus = merchantStatus,
            qrStatus = newQrStatus,
            lastVisitDate = today
        )
        CloudSyncManager.batchUpsertMerchantsToCloud(listOf(updatedMerchant))

        // Log audit trail
        val log = AuditLogEntity(
            userId = bdoUser.id,
            userName = bdoUser.name,
            action = "VISIT_SUBMITTED",
            entityType = "VISIT",
            entityId = visitId.toString(),
            dateStr = today,
            timeStr = DateUtils.formatTime(now),
            recordAffected = "Visit to ${merchant.shopName} (${merchant.merchantId})",
            metadata = "Visit to ${merchant.shopName} (${merchant.merchantId}) - $gpsStatus (${GeoUtils.formatDistance(distanceMeters)}) - QR: $newQrStatus (Tx: Rs. $transactionAmount)"
        )
        db.auditLogDao().insertLog(log)
        CloudSyncManager.recordAuditLogToCloud(log)

        return createdVisit
    }

    suspend fun syncOfflineVisits(): Int {
        val unsynced = db.visitDao().getUnsyncedVisits().firstOrNull() ?: emptyList()
        if (unsynced.isNotEmpty()) {
            val ids = unsynced.map { it.id }
            db.visitDao().markVisitsSynced(ids)
        }
        return unsynced.size
    }

    suspend fun updateVisitVerification(
        visitId: Long,
        newStatus: String, // "Verified", "Rejected", "Needs Review"
        notes: String,
        adminUser: UserEntity
    ) {
        val now = System.currentTimeMillis()
        db.visitDao().updateVerificationStatus(visitId, newStatus, notes)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = adminUser.id,
                userName = adminUser.name,
                action = "VERIFY_VISIT_$newStatus",
                entityType = "VISIT",
                entityId = visitId.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Visit #$visitId",
                previousValue = "Pending / Review",
                newValue = newStatus,
                metadata = "Verified by ${adminUser.name}. Notes: $notes"
            )
        )
    }

    // User Management for Master & Admin
    suspend fun createUser(user: UserEntity, creator: UserEntity): Long {
        val id = db.userDao().insertUser(user)
        val createdUser = user.copy(id = id)
        // Instantly sync newly created user account to centralized cloud database
        CloudSyncManager.saveUserToCloud(createdUser)

        val now = System.currentTimeMillis()
        val log = AuditLogEntity(
            userId = creator.id,
            userName = creator.name,
            action = "CREATE_USER",
            entityType = "USER",
            entityId = id.toString(),
            dateStr = DateUtils.getTodayDateString(),
            timeStr = DateUtils.formatTime(now),
            recordAffected = "${user.role}: ${user.name} (${user.username})",
            previousValue = "None",
            newValue = "Created with role ${user.role}, region ${user.regionId}",
            metadata = "Created user ${user.username} with role ${user.role}"
        )
        db.auditLogDao().insertLog(log)
        CloudSyncManager.recordAuditLogToCloud(log)
        return id
    }

    suspend fun updateUserStatus(userId: Long, newStatus: String, modifier: UserEntity) {
        val now = System.currentTimeMillis()
        db.userDao().updateUserStatus(userId, newStatus, now)
        val updatedUser = db.userDao().getUserById(userId)
        if (updatedUser != null) {
            CloudSyncManager.saveUserToCloud(updatedUser)
        }
        val log = AuditLogEntity(
            userId = modifier.id,
            userName = modifier.name,
            action = "UPDATE_USER_STATUS",
            entityType = "USER",
            entityId = userId.toString(),
            dateStr = DateUtils.getTodayDateString(),
            timeStr = DateUtils.formatTime(now),
            recordAffected = "User #$userId",
            previousValue = "Unknown",
            newValue = newStatus,
            metadata = "Status set to $newStatus by ${modifier.name}"
        )
        db.auditLogDao().insertLog(log)
        CloudSyncManager.recordAuditLogToCloud(log)
    }

    suspend fun reassignUserHierarchy(
        userId: Long,
        tlId: Long?,
        asmId: Long?,
        regionId: Long?,
        modifier: UserEntity
    ) {
        val now = System.currentTimeMillis()
        db.userDao().updateReporting(userId, tlId, asmId, regionId, now)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = modifier.id,
                userName = modifier.name,
                action = "REASSIGN_USER_HIERARCHY",
                entityType = "USER",
                entityId = userId.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "User #$userId",
                previousValue = "Previous Hierarchy",
                newValue = "Region: $regionId, ASM: $asmId, TL: $tlId",
                metadata = "Reassigned user hierarchy by ${modifier.name}"
            )
        )
    }

    suspend fun resetUserPassword(userId: Long, newPassHash: String, modifier: UserEntity) {
        val now = System.currentTimeMillis()
        db.userDao().updatePassword(userId, newPassHash, now)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = modifier.id,
                userName = modifier.name,
                action = "RESET_PASSWORD",
                entityType = "USER",
                entityId = userId.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "User #$userId",
                previousValue = "Encrypted Hash",
                newValue = "Reset to new hash",
                metadata = "Password reset by ${modifier.name}"
            )
        )
    }

    // Merchant management
    suspend fun createMerchant(merchant: MerchantEntity, modifier: UserEntity): Long {
        val id = db.merchantDao().insertMerchant(merchant)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = modifier.id,
                userName = modifier.name,
                action = "CREATE_MERCHANT",
                entityType = "MERCHANT",
                entityId = merchant.merchantId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Merchant: ${merchant.shopName} (${merchant.merchantId})",
                previousValue = "None",
                newValue = "Created with BDO ${merchant.bdoName}, TL ${merchant.tlName}",
                metadata = "Created merchant '${merchant.shopName}'"
            )
        )
        return id
    }

    suspend fun updateMerchant(merchant: MerchantEntity, modifier: UserEntity) {
        db.merchantDao().updateMerchant(merchant)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = modifier.id,
                userName = modifier.name,
                action = "UPDATE_MERCHANT",
                entityType = "MERCHANT",
                entityId = merchant.merchantId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Merchant: ${merchant.shopName} (${merchant.merchantId})",
                previousValue = "Existing record",
                newValue = "Updated details, QR ${merchant.qrStatus}, Status ${merchant.merchantStatus}",
                metadata = "Merchant details updated by ${modifier.name}"
            )
        )
    }

    suspend fun reassignMerchant(
        merchantId: Long,
        newBdoId: Long,
        newTlId: Long,
        newRegionId: Long,
        adminUser: UserEntity
    ) {
        db.merchantDao().reassignMerchant(merchantId, newBdoId, newTlId, newRegionId)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = adminUser.id,
                userName = adminUser.name,
                action = "REASSIGN_MERCHANT",
                entityType = "MERCHANT",
                entityId = merchantId.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Merchant #$merchantId",
                previousValue = "Old assignment",
                newValue = "BDO $newBdoId, TL $newTlId, Region $newRegionId",
                metadata = "Reassigned to BDO $newBdoId, TL $newTlId, Region $newRegionId"
            )
        )
    }

    /**
     * Excel import handling both new creation and existing merchant updates.
     */
    suspend fun importMerchantsUpsert(
        merchants: List<MerchantEntity>,
        modifier: UserEntity
    ) {
        val now = System.currentTimeMillis()
        for (m in merchants) {
            val existing = db.merchantDao().findMerchantByMerchantId(m.merchantId)
            if (existing != null) {
                // Update existing merchant preserving primary key id
                val updated = m.copy(
                    id = existing.id,
                    createdAt = existing.createdAt,
                    updatedAt = now
                )
                db.merchantDao().updateMerchant(updated)
            } else {
                db.merchantDao().insertMerchant(m)
            }
        }

        // Real-time batch upsert to Firestore (merge: true) - instantly pushes to all BDO devices
        CloudSyncManager.batchUpsertMerchantsToCloud(merchants)

        val log = AuditLogEntity(
            userId = modifier.id,
            userName = modifier.name,
            action = "EXCEL_IMPORT_UPSERT",
            entityType = "MERCHANTS",
            entityId = merchants.size.toString(),
            dateStr = DateUtils.getTodayDateString(),
            timeStr = DateUtils.formatTime(now),
            recordAffected = "${merchants.size} Merchants",
            previousValue = "Various",
            newValue = "Bulk imported & updated to Cloud + Local",
            metadata = "Batch upserted ${merchants.size} merchants to real-time central cloud"
        )
        db.auditLogDao().insertLog(log)
        CloudSyncManager.recordAuditLogToCloud(log)
    }

    suspend fun upsertTarget(target: TargetEntity, adminUser: UserEntity) {
        db.targetDao().insertTarget(target)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = adminUser.id,
                userName = adminUser.name,
                action = "UPSERT_TARGET",
                entityType = "TARGET",
                entityId = "${target.bdoId}_${target.dateString}",
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Target for BDO ${target.bdoId} on ${target.dateString}",
                previousValue = "Previous Target",
                newValue = "V=${target.visitTarget}, O=${target.onboardingTarget}, Q=${target.qrTarget}",
                metadata = "Target set: V=${target.visitTarget}, O=${target.onboardingTarget}, Q=${target.qrTarget}, A=${target.activationTarget}"
            )
        )
    }

    suspend fun createRegion(name: String, adminUser: UserEntity): Long {
        val id = db.regionDao().insertRegion(RegionEntity(name = name.trim()))
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = adminUser.id,
                userName = adminUser.name,
                action = "CREATE_REGION",
                entityType = "REGION",
                entityId = id.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Region: $name",
                previousValue = "None",
                newValue = "Created with ID $id",
                metadata = "Created region '$name'"
            )
        )
        return id
    }

    suspend fun createTl(name: String, regionId: Long, adminUser: UserEntity): Long {
        val id = db.tlDao().insertTl(TlEntity(name = name.trim(), regionId = regionId))
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = adminUser.id,
                userName = adminUser.name,
                action = "CREATE_TL",
                entityType = "TL",
                entityId = id.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "TL: $name in Region $regionId",
                previousValue = "None",
                newValue = "Created with ID $id",
                metadata = "Created TL '$name' in region $regionId"
            )
        )
        return id
    }

    // Soft delete user (Requirement 7: Master can delete accounts, preserving historical records)
    suspend fun deleteUser(userId: Long, modifier: UserEntity) {
        val now = System.currentTimeMillis()
        db.userDao().updateUserStatus(userId, "Deleted", now)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = modifier.id,
                userName = modifier.name,
                action = "DELETE_USER",
                entityType = "USER",
                entityId = userId.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "User #$userId",
                previousValue = "Active",
                newValue = "Deleted",
                metadata = "Master deleted user #$userId (soft-delete). Login revoked, visits/records preserved."
            )
        )
    }

    suspend fun updateUser(user: UserEntity, modifier: UserEntity) {
        val now = System.currentTimeMillis()
        db.userDao().updateUser(user.copy(updatedAt = now))
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = modifier.id,
                userName = modifier.name,
                action = "UPDATE_USER",
                entityType = "USER",
                entityId = user.id.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "User: ${user.name} (${user.username})",
                previousValue = "User profile",
                newValue = "Role: ${user.role}, Status: ${user.status}, Region: ${user.regionId}",
                metadata = "Account details updated by Master ${modifier.name}"
            )
        )
    }

    // QR Operations (Requirements 24 to 35)
    fun getQrRequestsForAsm(asmId: Long): Flow<List<QrRequestEntity>> =
        db.qrRequestDao().getQrRequestsByAsm(asmId)

    fun getQrRequestsForTl(tlId: Long): Flow<List<QrRequestEntity>> =
        db.qrRequestDao().getQrRequestsByTl(tlId)

    fun getQrRequestsForBdo(bdoId: Long): Flow<List<QrRequestEntity>> =
        db.qrRequestDao().getQrRequestsByBdo(bdoId)

    suspend fun createQrRequest(request: QrRequestEntity, actor: UserEntity): Long {
        val id = db.qrRequestDao().insertQrRequest(request)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = actor.id,
                userName = actor.name,
                action = "CREATE_QR_REQUEST",
                entityType = "QR_REQUEST",
                entityId = request.requestId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Request ${request.requestId} for ${request.merchantName}",
                previousValue = "None",
                newValue = "Status: ${request.status}, Urgent: ${request.isUrgent}",
                metadata = "Created ${request.requestType} for ${request.merchantName} (${request.merchantId})"
            )
        )
        return id
    }

    suspend fun updateQrRequestStatus(requestId: Long, newStatus: String, actor: UserEntity) {
        db.qrRequestDao().updateStatus(requestId, newStatus)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = actor.id,
                userName = actor.name,
                action = "UPDATE_QR_STATUS",
                entityType = "QR_REQUEST",
                entityId = requestId.toString(),
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "QR Request #$requestId",
                previousValue = "Status update",
                newValue = newStatus,
                metadata = "Status set to $newStatus by ${actor.name}"
            )
        )
    }

    suspend fun forwardQrToVendor(
        request: QrRequestEntity,
        vendorDate: String,
        vendorRef: String,
        coordinatorName: String,
        actor: UserEntity
    ) {
        val updated = request.copy(
            status = "FORWARDED_TO_VENDOR",
            vendorForwardedDate = vendorDate,
            vendorReference = vendorRef,
            coordinatorName = coordinatorName,
            updatedAt = System.currentTimeMillis()
        )
        db.qrRequestDao().updateQrRequest(updated)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = actor.id,
                userName = actor.name,
                action = "QR_FORWARDED_TO_VENDOR",
                entityType = "QR_REQUEST",
                entityId = request.requestId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Request ${request.requestId}",
                previousValue = request.status,
                newValue = "FORWARDED_TO_VENDOR",
                metadata = "Forwarded to vendor with ref $vendorRef by coordinator $coordinatorName"
            )
        )
    }

    suspend fun updateQrScanning(
        request: QrRequestEntity,
        scanningStatus: String,
        discrepancy: String,
        actor: UserEntity
    ) {
        val newStatus = if (discrepancy.isNotBlank()) "DISPUTED" else "SCANNED"
        val updated = request.copy(
            scanningStatus = scanningStatus,
            scanningDiscrepancy = discrepancy,
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        db.qrRequestDao().updateQrRequest(updated)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = actor.id,
                userName = actor.name,
                action = "QR_SCANNING_UPDATED",
                entityType = "QR_REQUEST",
                entityId = request.requestId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Request ${request.requestId}",
                previousValue = request.status,
                newValue = newStatus,
                metadata = "Scanning: $scanningStatus, Discrepancy: $discrepancy"
            )
        )
    }

    suspend fun updateQrCourier(
        request: QrRequestEntity,
        courierName: String,
        trackingNumber: String,
        dispatchDate: String,
        expectedDeliveryDate: String,
        actor: UserEntity
    ) {
        val updated = request.copy(
            courierName = courierName,
            trackingNumber = trackingNumber,
            dispatchDate = dispatchDate,
            expectedDeliveryDate = expectedDeliveryDate,
            deliveryStatus = "In Transit",
            status = "DISPATCHED",
            updatedAt = System.currentTimeMillis()
        )
        db.qrRequestDao().updateQrRequest(updated)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = actor.id,
                userName = actor.name,
                action = "QR_DISPATCHED",
                entityType = "QR_REQUEST",
                entityId = request.requestId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Request ${request.requestId}",
                previousValue = request.status,
                newValue = "DISPATCHED",
                metadata = "Courier: $courierName, Tracking: $trackingNumber, Expected: $expectedDeliveryDate"
            )
        )
    }

    suspend fun confirmQrDelivery(
        request: QrRequestEntity,
        actualDeliveryDate: String,
        actor: UserEntity
    ) {
        val updated = request.copy(
            actualDeliveryDate = actualDeliveryDate,
            deliveryStatus = "Delivered",
            status = "DELIVERED",
            updatedAt = System.currentTimeMillis()
        )
        db.qrRequestDao().updateQrRequest(updated)
        val now = System.currentTimeMillis()
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = actor.id,
                userName = actor.name,
                action = "QR_DELIVERY_CONFIRMED",
                entityType = "QR_REQUEST",
                entityId = request.requestId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Request ${request.requestId}",
                previousValue = request.status,
                newValue = "DELIVERED",
                metadata = "Delivery confirmed on $actualDeliveryDate by ${actor.name}"
            )
        )
    }

    suspend fun escalateQrRequest(
        request: QrRequestEntity,
        escalationComments: String,
        actor: UserEntity
    ) {
        val now = System.currentTimeMillis()
        val updated = request.copy(
            isEscalated = true,
            escalationComments = escalationComments,
            escalationTimestamp = now,
            status = "ESCALATED",
            updatedAt = now
        )
        db.qrRequestDao().updateQrRequest(updated)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = actor.id,
                userName = actor.name,
                action = "QR_ESCALATED",
                entityType = "QR_REQUEST",
                entityId = request.requestId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Request ${request.requestId}",
                previousValue = request.status,
                newValue = "ESCALATED",
                metadata = "Escalated by ${actor.name}: $escalationComments"
            )
        )
    }

    suspend fun submitNonDeploymentJustification(
        request: QrRequestEntity,
        justification: String,
        actor: UserEntity
    ) {
        val now = System.currentTimeMillis()
        val updated = request.copy(
            nonDeploymentJustification = justification,
            justificationUser = actor.name,
            justificationTimestamp = now,
            updatedAt = now
        )
        db.qrRequestDao().updateQrRequest(updated)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                userId = actor.id,
                userName = actor.name,
                action = "QR_NON_DEPLOYMENT_JUSTIFICATION",
                entityType = "QR_REQUEST",
                entityId = request.requestId,
                dateStr = DateUtils.getTodayDateString(),
                timeStr = DateUtils.formatTime(now),
                recordAffected = "Request ${request.requestId}",
                previousValue = "None",
                newValue = justification,
                metadata = "Justification submitted by ${actor.name}: $justification"
            )
        )
    }
}
