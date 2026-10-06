package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseBackupManager {

    /**
     * Executes a full WAL checkpoint to ensure all uncommitted writes in WAL
     * are synchronized into the main .db SQLite file on disk.
     */
    suspend fun checkpointWal(db: AppDatabase) = withContext(Dispatchers.IO) {
        try {
            val cursor = db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
            cursor.moveToFirst()
            cursor.close()
        } catch (_: Exception) {
            // Ignore if in-memory or WAL disabled
        }
    }

    /**
     * Safely creates a standalone copy of the SQLite database file (.db)
     * suitable for Google Drive upload or manual device backup.
     */
    suspend fun exportDatabaseFile(context: Context, db: AppDatabase): File = withContext(Dispatchers.IO) {
        checkpointWal(db)
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val targetFile = File(backupDir, "qr_merchant_field_force_$timeStamp.db")

        FileInputStream(dbFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    }

    /**
     * Exports all database entities into a structured JSON string.
     */
    suspend fun exportDataAsJson(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 3)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

        // Merchants
        val merchants = db.merchantDao().getAllMerchants().firstOrNull() ?: emptyList()
        val merchantsArray = JSONArray()
        for (m in merchants) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("merchantId", m.merchantId)
                put("merchantName", m.merchantName)
                put("shopName", m.shopName)
                put("mobile", m.mobile)
                put("address", m.address)
                put("city", m.city)
                put("bdoId", m.bdoId)
                put("bdoName", m.bdoName)
                put("latitude", m.latitude)
                put("longitude", m.longitude)
                put("qrId", m.qrId)
                put("qrStatus", m.qrStatus)
                put("merchantStatus", m.merchantStatus)
                put("merchantCategory", m.merchantCategory)
            }
            merchantsArray.put(obj)
        }
        root.put("merchants", merchantsArray)

        // Visits
        val visits = db.visitDao().getAllVisits().firstOrNull() ?: emptyList()
        val visitsArray = JSONArray()
        for (v in visits) {
            val obj = JSONObject().apply {
                put("id", v.id)
                put("merchantId", v.merchantId)
                put("merchantName", v.merchantName)
                put("bdoId", v.bdoId)
                put("bdoName", v.bdoName)
                put("visitDateString", v.visitDateString)
                put("latitude", v.latitude)
                put("longitude", v.longitude)
                put("gpsStatus", v.gpsStatus)
                put("visitStatus", v.visitStatus)
                put("qrDeployed", v.qrDeployed)
                put("visitRemarks", v.visitRemarks)
            }
            visitsArray.put(obj)
        }
        root.put("visits", visitsArray)

        // Audit Logs
        val auditLogs = db.auditLogDao().getAllAuditLogs().firstOrNull() ?: emptyList()
        val auditArray = JSONArray()
        for (a in auditLogs) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("userId", a.userId)
                put("userName", a.userName)
                put("action", a.action)
                put("entityType", a.entityType)
                put("entityId", a.entityId)
                put("dateStr", a.dateStr)
                put("timeStr", a.timeStr)
                put("recordAffected", a.recordAffected)
                put("newValue", a.newValue)
            }
            auditArray.put(obj)
        }
        root.put("auditLogs", auditArray)

        root.toString(2)
    }

    /**
     * Exports merchants to CSV format.
     */
    suspend fun exportMerchantsCsv(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val merchants = db.merchantDao().getAllMerchants().firstOrNull() ?: emptyList()
        val sb = StringBuilder()
        sb.append("User ID,Merchant ID,Merchant Name,Shop Name,Mobile,Address,City,Latitude,Longitude,QR ID,Category,Status,QR Status\n")
        for (m in merchants) {
            sb.append("\"${m.bdoId}\",")
            sb.append("\"${m.merchantId}\",")
            sb.append("\"${m.merchantName.replace("\"", "\"\"")}\",")
            sb.append("\"${m.shopName.replace("\"", "\"\"")}\",")
            sb.append("\"${m.mobile}\",")
            sb.append("\"${m.address.replace("\"", "\"\"")}\",")
            sb.append("\"${m.city}\",")
            sb.append("${m.latitude},")
            sb.append("${m.longitude},")
            sb.append("\"${m.qrId}\",")
            sb.append("\"${m.merchantCategory}\",")
            sb.append("\"${m.merchantStatus}\",")
            sb.append("\"${m.qrStatus}\"\n")
        }
        sb.toString()
    }

    /**
     * Restores database file from an external source or backup file.
     */
    suspend fun restoreDatabaseFromFile(context: Context, backupFileUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentDbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val currentWal = File(currentDbFile.path + "-wal")
            val currentShm = File(currentDbFile.path + "-shm")

            if (currentWal.exists()) currentWal.delete()
            if (currentShm.exists()) currentShm.delete()

            context.contentResolver.openInputStream(backupFileUri)?.use { input ->
                FileOutputStream(currentDbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            true
        } catch (_: Exception) {
            false
        }
    }
}
