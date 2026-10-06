package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DriveSyncResult(
    val success: Boolean,
    val message: String,
    val fileId: String? = null,
    val folderId: String? = null,
    val uploadedAt: Long = System.currentTimeMillis()
)

object GoogleDriveSyncService {

    private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    private const val DEFAULT_FOLDER_NAME = "QR_Merchant_Backups"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Finds or creates the target folder in the user's Google Drive.
     */
    suspend fun getOrCreateFolder(
        accessToken: String,
        folderName: String = DEFAULT_FOLDER_NAME
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Check if folder already exists
            val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val searchUrl = "$DRIVE_FILES_URL?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)"

            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(searchRequest).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        val folderId = files.getJSONObject(0).getString("id")
                        return@withContext Result.success(folderId)
                    }
                }
            }

            // 2. Create the folder if not found
            val metadataJson = JSONObject().apply {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
            }.toString()

            val createRequest = Request.Builder()
                .url(DRIVE_FILES_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            httpClient.newCall(createRequest).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val createdId = json.getString("id")
                    Result.success(createdId)
                } else {
                    Result.failure(Exception("Failed to create Drive folder: ${response.code} - $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads a file (SQLite DB or CSV) directly to the specified Google Drive folder.
     */
    suspend fun uploadFile(
        file: File,
        fileName: String,
        mimeType: String,
        parentFolderId: String,
        accessToken: String
    ): Result<DriveSyncResult> = withContext(Dispatchers.IO) {
        try {
            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("parents", org.json.JSONArray().apply { put(parentFolderId) })
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(
                    metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addPart(
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(DRIVE_UPLOAD_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val fileId = json.optString("id", "")
                    Result.success(
                        DriveSyncResult(
                            success = true,
                            message = "Successfully uploaded '$fileName' to Google Drive.",
                            fileId = fileId,
                            folderId = parentFolderId
                        )
                    )
                } else {
                    Result.failure(Exception("Upload failed (${response.code}): $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports and uploads complete SQLite database + CSV data dump to Google Drive.
     */
    suspend fun performFullBackupToDrive(
        context: Context,
        db: AppDatabase,
        accessToken: String
    ): DriveSyncResult = withContext(Dispatchers.IO) {
        try {
            // 1. Get or create target backup folder
            val folderResult = getOrCreateFolder(accessToken, DEFAULT_FOLDER_NAME)
            val folderId = folderResult.getOrElse {
                return@withContext DriveSyncResult(
                    success = false,
                    message = "Could not access or create Google Drive folder: ${it.message}"
                )
            }

            // 2. Export local SQLite database
            val dbFile = DatabaseBackupManager.exportDatabaseFile(context, db)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val uploadResult = uploadFile(
                file = dbFile,
                fileName = "QR_Merchant_DB_$timeStamp.db",
                mimeType = "application/x-sqlite3",
                parentFolderId = folderId,
                accessToken = accessToken
            )

            // 3. Export CSV merchant roster
            val csvContent = DatabaseBackupManager.exportMerchantsCsv(db)
            val csvFile = File(context.cacheDir, "merchants_roster_$timeStamp.csv")
            csvFile.writeText(csvContent)
            uploadFile(
                file = csvFile,
                fileName = "merchants_roster_$timeStamp.csv",
                mimeType = "text/csv",
                parentFolderId = folderId,
                accessToken = accessToken
            )

            // Cleanup local temp export
            if (csvFile.exists()) csvFile.delete()

            uploadResult.getOrElse {
                DriveSyncResult(
                    success = false,
                    message = "Drive upload error: ${it.message}"
                )
            }
        } catch (e: Exception) {
            DriveSyncResult(
                success = false,
                message = "Backup error: ${e.message}"
            )
        }
    }

    /**
     * Posts backup payload to backend route /api/backup/gdrive.
     */
    suspend fun syncToBackendServer(
        backendUrl: String,
        jsonData: String,
        bearerToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (backendUrl.endsWith("/api/backup/gdrive")) {
                backendUrl
            } else {
                backendUrl.trimEnd('/') + "/api/backup/gdrive"
            }

            val requestBuilder = Request.Builder()
                .url(fullUrl)
                .post(jsonData.toRequestBody("application/json".toMediaType()))

            if (!bearerToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $bearerToken")
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(bodyStr)
                } else {
                    Result.failure(Exception("Backend sync error (${response.code}): $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
