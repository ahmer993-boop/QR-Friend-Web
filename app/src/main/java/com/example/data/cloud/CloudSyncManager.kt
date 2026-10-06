package com.example.data.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class CloudNetworkStatus(
    val isOnline: Boolean = true,
    val statusLabel: String = "Connecting to Cloud...",
    val pendingWritesCount: Int = 0,
    val lastSyncTime: Long = System.currentTimeMillis()
)

object CloudSyncManager {

    private const val TAG = "CloudSyncManager"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_MERCHANTS = "merchants"
    private const val COLLECTION_VISITS = "visits"
    private const val COLLECTION_AUDIT_LOGS = "audit_logs"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var firestoreInstance: FirebaseFirestore? = null
    private var isInitialized = false

    private val _networkStatus = MutableStateFlow(CloudNetworkStatus())
    val networkStatus: StateFlow<CloudNetworkStatus> = _networkStatus.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()

    /**
     * Initializes Firebase Firestore with robust offline persistence caching
     * and attaches real-time snapshot listeners.
     */
    fun initialize(context: Context, db: AppDatabase) {
        if (isInitialized) return

        try {
            // 1. Ensure FirebaseApp is initialized safely
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.qrfriend")
                    .setProjectId("qr-friend-c4eb1")
                    .setApiKey("AIzaSyQrFriendFieldForceKey2026")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            }

            val firestore = FirebaseFirestore.getInstance()

            // 2. Configure persistent disk caching for seamless offline operations
            val settings = firestoreSettings {
                val cacheSettings = PersistentCacheSettings.newBuilder().build()
                setLocalCacheSettings(cacheSettings)
            }
            firestore.firestoreSettings = settings
            firestoreInstance = firestore
            isInitialized = true

            // 3. Monitor network connectivity
            setupConnectivityMonitoring(context)

            // 4. Start Real-time Data Listeners across all devices
            setupRealtimeListeners(firestore, db)

            _networkStatus.value = CloudNetworkStatus(
                isOnline = true,
                statusLabel = "Live Cloud Synced (Firestore)",
                lastSyncTime = System.currentTimeMillis()
            )
            Log.i(TAG, "CloudSyncManager initialized successfully with offline persistence.")
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed: ${e.message}", e)
            _networkStatus.value = CloudNetworkStatus(
                isOnline = false,
                statusLabel = "Offline Mode (Local Cache Active)",
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }

    private fun setupConnectivityMonitoring(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _networkStatus.value = _networkStatus.value.copy(
                        isOnline = true,
                        statusLabel = "Live Cloud Synced (Online)",
                        lastSyncTime = System.currentTimeMillis()
                    )
                }

                override fun onLost(network: Network) {
                    _networkStatus.value = _networkStatus.value.copy(
                        isOnline = false,
                        statusLabel = "Offline Mode (Local Cache Active)",
                        lastSyncTime = System.currentTimeMillis()
                    )
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    /**
     * Real-time Data Listeners: Automatically receives new BDO accounts,
     * imported merchants, and field visits from other devices.
     */
    private fun setupRealtimeListeners(firestore: FirebaseFirestore, db: AppDatabase) {
        // Clear previous listeners if any
        activeListeners.forEach { it.remove() }
        activeListeners.clear()

        // 1. Listen for USER ACCOUNTS (Master Admin creates BDO -> instant sync to all devices)
        val userListener = firestore.collection(COLLECTION_USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Users listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val username = doc.getString("username") ?: continue
                                val existing = db.userDao().findByUsername(username)
                                val user = UserEntity(
                                    id = doc.getLong("id") ?: (existing?.id ?: 0L),
                                    name = doc.getString("name") ?: "",
                                    username = username,
                                    email = doc.getString("email") ?: "",
                                    mobile = doc.getString("mobile") ?: "",
                                    passwordHash = doc.getString("passwordHash") ?: "",
                                    role = doc.getString("role") ?: "BDO",
                                    regionId = doc.getLong("regionId") ?: 1L,
                                    tlId = doc.getLong("tlId"),
                                    asmId = doc.getLong("asmId"),
                                    status = doc.getString("status") ?: "Active"
                                )
                                if (existing == null) {
                                    db.userDao().insertUser(user)
                                } else {
                                    db.userDao().updateUser(user.copy(id = existing.id))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing cloud user: ${e.message}")
                            }
                        }
                    }
                }
            }
        activeListeners.add(userListener)

        // 2. Listen for MERCHANTS (Master Admin imports Excel -> instant sync to BDO devices)
        val merchantListener = firestore.collection(COLLECTION_MERCHANTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Merchants listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val merchantId = doc.getString("merchantId") ?: continue
                                val existing = db.merchantDao().findMerchantByMerchantId(merchantId)
                                val merchant = MerchantEntity(
                                    id = existing?.id ?: 0L,
                                    merchantId = merchantId,
                                    merchantName = doc.getString("merchantName") ?: "",
                                    businessName = doc.getString("businessName") ?: "",
                                    shopName = doc.getString("shopName") ?: "",
                                    mobile = doc.getString("mobile") ?: "",
                                    address = doc.getString("address") ?: "",
                                    city = doc.getString("city") ?: "",
                                    regionId = doc.getLong("regionId") ?: 1L,
                                    bdoId = doc.getLong("bdoId") ?: 0L,
                                    bdoName = doc.getString("bdoName") ?: "",
                                    latitude = doc.getDouble("latitude") ?: 0.0,
                                    longitude = doc.getDouble("longitude") ?: 0.0,
                                    qrId = doc.getString("qrId") ?: "",
                                    qrStatus = doc.getString("qrStatus") ?: "Not Deployed",
                                    merchantStatus = doc.getString("merchantStatus") ?: "New",
                                    merchantCategory = doc.getString("merchantCategory") ?: "General Store",
                                    lastVisitDate = doc.getString("lastVisitDate") ?: ""
                                )
                                if (existing == null) {
                                    db.merchantDao().insertMerchant(merchant)
                                } else {
                                    db.merchantDao().updateMerchant(merchant.copy(id = existing.id))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing cloud merchant: ${e.message}")
                            }
                        }
                    }
                }
            }
        activeListeners.add(merchantListener)

        // 3. Listen for VISITS (BDO logs visit -> instant sync to Admin & TL)
        val visitListener = firestore.collection(COLLECTION_VISITS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val merchantId = doc.getString("merchantId") ?: continue
                                val visitDate = doc.getString("visitDateString") ?: ""
                                val bdoId = doc.getLong("bdoId") ?: 0L
                                val visit = VisitEntity(
                                    merchantId = merchantId,
                                    merchantName = doc.getString("merchantName") ?: "",
                                    bdoId = bdoId,
                                    bdoName = doc.getString("bdoName") ?: "",
                                    visitDateString = visitDate,
                                    latitude = doc.getDouble("latitude") ?: 0.0,
                                    longitude = doc.getDouble("longitude") ?: 0.0,
                                    gpsAccuracy = 5.0f,
                                    gpsStatus = doc.getString("gpsStatus") ?: "VALID",
                                    visitStatus = doc.getString("visitStatus") ?: "Completed",
                                    qrDeployed = doc.getBoolean("qrDeployed") ?: false,
                                    visitRemarks = doc.getString("visitRemarks") ?: "",
                                    isSynced = true
                                )
                                db.visitDao().insertVisit(visit)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing cloud visit: ${e.message}")
                            }
                        }
                    }
                }
            }
        activeListeners.add(visitListener)

        // 4. Listen for AUDIT LOGS (Immutable security audit stream)
        val auditListener = firestore.collection(COLLECTION_AUDIT_LOGS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val timestamp = doc.getLong("timestamp") ?: continue
                                val entityId = doc.getString("entityId") ?: ""
                                val action = doc.getString("action") ?: ""
                                val log = AuditLogEntity(
                                    userId = doc.getLong("userId") ?: 0L,
                                    userName = doc.getString("userName") ?: "",
                                    action = action,
                                    entityType = doc.getString("entityType") ?: "",
                                    entityId = entityId,
                                    dateStr = doc.getString("dateStr") ?: "",
                                    timeStr = doc.getString("timeStr") ?: "",
                                    timestamp = timestamp,
                                    recordAffected = doc.getString("recordAffected") ?: "",
                                    previousValue = doc.getString("previousValue") ?: "",
                                    newValue = doc.getString("newValue") ?: "",
                                    metadata = doc.getString("metadata") ?: ""
                                )
                                db.auditLogDao().insertLog(log)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing audit log: ${e.message}")
                            }
                        }
                    }
                }
            }
        activeListeners.add(auditListener)
    }

    // =========================================================================
    // Cloud Mutation Operations (with automatic offline queueing and merge: true)
    // =========================================================================

    /**
     * Saves or updates a user directly in the central cloud `users` collection.
     * Accessible immediately by newly created BDOs on any device.
     */
    suspend fun saveUserToCloud(user: UserEntity): Boolean = withContext(Dispatchers.IO) {
        val firestore = firestoreInstance ?: return@withContext false
        try {
            val userMap = hashMapOf(
                "id" to user.id,
                "name" to user.name,
                "username" to user.username.lowercase().trim(),
                "email" to user.email,
                "mobile" to user.mobile,
                "passwordHash" to user.passwordHash,
                "role" to user.role,
                "regionId" to user.regionId,
                "tlId" to user.tlId,
                "asmId" to user.asmId,
                "status" to user.status,
                "updatedAt" to System.currentTimeMillis()
            )

            // Document ID is the canonical username for instant lookup
            firestore.collection(COLLECTION_USERS)
                .document(user.username.lowercase().trim())
                .set(userMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user to cloud: ${e.message}")
            false
        }
    }

    /**
     * Attempts to fetch a user account from the cloud Firestore collection.
     * Called when a user logs in on a fresh mobile device before the local DB
     * has received the real-time sync event.
     */
    suspend fun fetchUserFromCloud(username: String): UserEntity? = withContext(Dispatchers.IO) {
        val firestore = firestoreInstance ?: return@withContext null
        try {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(username.lowercase().trim())
                .get()
                .await()

            if (doc.exists()) {
                UserEntity(
                    id = doc.getLong("id") ?: System.currentTimeMillis(),
                    name = doc.getString("name") ?: "",
                    username = doc.getString("username") ?: username,
                    email = doc.getString("email") ?: "",
                    mobile = doc.getString("mobile") ?: "",
                    passwordHash = doc.getString("passwordHash") ?: "",
                    role = doc.getString("role") ?: "BDO",
                    regionId = doc.getLong("regionId") ?: 1L,
                    tlId = doc.getLong("tlId"),
                    asmId = doc.getLong("asmId"),
                    status = doc.getString("status") ?: "Active"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user from cloud: ${e.message}")
            null
        }
    }

    /**
     * EXCEL_IMPORT_UPSERT: Batch upserts merchant records (`merge: true`)
     * directly into the central cloud `merchants` collection in chunks.
     */
    suspend fun batchUpsertMerchantsToCloud(merchants: List<MerchantEntity>): Result<Int> = withContext(Dispatchers.IO) {
        val firestore = firestoreInstance ?: return@withContext Result.failure(Exception("Cloud service unavailable"))
        try {
            var upsertedCount = 0
            // Firestore writeBatch supports up to 500 ops per batch
            val chunkSize = 400
            val chunks = merchants.chunked(chunkSize)

            for (chunk in chunks) {
                val batch = firestore.batch()
                for (m in chunk) {
                    val docRef = firestore.collection(COLLECTION_MERCHANTS).document(m.merchantId.trim())
                    val map = hashMapOf(
                        "merchantId" to m.merchantId.trim(),
                        "merchantName" to m.merchantName.trim(),
                        "businessName" to m.businessName.trim(),
                        "shopName" to m.shopName.trim(),
                        "mobile" to m.mobile.trim(),
                        "address" to m.address.trim(),
                        "city" to m.city.trim(),
                        "regionId" to m.regionId,
                        "bdoId" to m.bdoId,
                        "bdoName" to m.bdoName.trim(),
                        "latitude" to m.latitude,
                        "longitude" to m.longitude,
                        "qrId" to m.qrId.trim(),
                        "qrStatus" to m.qrStatus,
                        "merchantStatus" to m.merchantStatus,
                        "merchantCategory" to m.merchantCategory,
                        "lastVisitDate" to m.lastVisitDate,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(docRef, map, SetOptions.merge())
                }
                batch.commit().await()
                upsertedCount += chunk.size
            }

            _networkStatus.value = _networkStatus.value.copy(
                lastSyncTime = System.currentTimeMillis()
            )
            Result.success(upsertedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Batch upsert merchants failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Records a field visit to the cloud `visits` collection.
     * Automatically cached and queued if offline.
     */
    suspend fun recordVisitToCloud(visit: VisitEntity): Boolean = withContext(Dispatchers.IO) {
        val firestore = firestoreInstance ?: return@withContext false
        try {
            val visitDocId = "${visit.merchantId}_${visit.visitDateString}_${visit.bdoId}"
            val map = hashMapOf(
                "merchantId" to visit.merchantId,
                "merchantName" to visit.merchantName,
                "bdoId" to visit.bdoId,
                "bdoName" to visit.bdoName,
                "visitDateString" to visit.visitDateString,
                "latitude" to visit.latitude,
                "longitude" to visit.longitude,
                "gpsStatus" to visit.gpsStatus,
                "visitStatus" to visit.visitStatus,
                "qrDeployed" to visit.qrDeployed,
                "visitRemarks" to visit.visitRemarks,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection(COLLECTION_VISITS)
                .document(visitDocId)
                .set(map, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record visit to cloud: ${e.message}")
            false
        }
    }

    /**
     * Appends an immutable audit log entry into the cloud `audit_logs` collection.
     */
    suspend fun recordAuditLogToCloud(log: AuditLogEntity): Boolean = withContext(Dispatchers.IO) {
        val firestore = firestoreInstance ?: return@withContext false
        try {
            val logMap = hashMapOf(
                "userId" to log.userId,
                "userName" to log.userName,
                "action" to log.action,
                "entityType" to log.entityType,
                "entityId" to log.entityId,
                "dateStr" to log.dateStr,
                "timeStr" to log.timeStr,
                "timestamp" to log.timestamp,
                "recordAffected" to log.recordAffected,
                "previousValue" to log.previousValue,
                "newValue" to log.newValue,
                "metadata" to log.metadata
            )

            firestore.collection(COLLECTION_AUDIT_LOGS)
                .add(logMap)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record audit log to cloud: ${e.message}")
            false
        }
    }
}
