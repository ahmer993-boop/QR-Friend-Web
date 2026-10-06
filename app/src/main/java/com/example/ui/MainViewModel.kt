package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.data.cloud.CloudNetworkStatus
import com.example.data.cloud.CloudSyncManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.QrRequestEntity
import com.example.data.local.entity.RegionEntity
import com.example.data.local.entity.TargetEntity
import com.example.data.local.entity.TlEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.VisitEntity
import com.example.data.repository.AppRepository
import com.example.ui.common.AdminTab
import com.example.ui.common.AsmTab
import com.example.ui.common.BdoTab
import com.example.ui.common.MasterTab
import com.example.ui.common.TlTab
import com.example.util.DateUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _masterTab = MutableStateFlow(MasterTab.DASHBOARD)
    val masterTab: StateFlow<MasterTab> = _masterTab.asStateFlow()

    private val _adminTab = MutableStateFlow(AdminTab.DASHBOARD)
    val adminTab: StateFlow<AdminTab> = _adminTab.asStateFlow()

    private val _asmTab = MutableStateFlow(AsmTab.DASHBOARD)
    val asmTab: StateFlow<AsmTab> = _asmTab.asStateFlow()

    private val _tlTab = MutableStateFlow(TlTab.DASHBOARD)
    val tlTab: StateFlow<TlTab> = _tlTab.asStateFlow()

    private val _bdoTab = MutableStateFlow(BdoTab.HOME)
    val bdoTab: StateFlow<BdoTab> = _bdoTab.asStateFlow()

    private val _activeMerchantForVisit = MutableStateFlow<MerchantEntity?>(null)
    val activeMerchantForVisit: StateFlow<MerchantEntity?> = _activeMerchantForVisit.asStateFlow()

    private val _selectedMerchantForDetail = MutableStateFlow<MerchantEntity?>(null)
    val selectedMerchantForDetail: StateFlow<MerchantEntity?> = _selectedMerchantForDetail.asStateFlow()

    private val _selectedVisitForDetail = MutableStateFlow<VisitEntity?>(null)
    val selectedVisitForDetail: StateFlow<VisitEntity?> = _selectedVisitForDetail.asStateFlow()

    val cloudStatus: StateFlow<CloudNetworkStatus> = CloudSyncManager.networkStatus

    fun triggerCloudSync() {
        viewModelScope.launch {
            repository.syncOfflineVisits()
        }
    }

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _userActionMessage = MutableStateFlow<String?>(null)
    val userActionMessage: StateFlow<String?> = _userActionMessage.asStateFlow()

    fun clearUserActionMessage() {
        _userActionMessage.value = null
    }

    val allMerchants = repository.allMerchants.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allVisits = repository.allVisits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val verificationQueue = repository.verificationQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRegions = repository.allRegions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTls = repository.allTls.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTargets = repository.allTargets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAuditLogs = repository.allAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unsyncedVisits = repository.unsyncedVisits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allQrRequests = repository.allQrRequests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // BDO filtered data
    val bdoMerchants: StateFlow<List<MerchantEntity>> = combine(_currentUser, repository.allMerchants) { user, merchants ->
        if (user == null || user.role == "MASTER" || user.role == "ADMIN") merchants
        else merchants.filter { it.bdoId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bdoVisits: StateFlow<List<VisitEntity>> = combine(_currentUser, repository.allVisits) { user, visits ->
        if (user == null || user.role == "MASTER" || user.role == "ADMIN") visits
        else visits.filter { it.bdoId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bdoQrRequests: StateFlow<List<QrRequestEntity>> = combine(_currentUser, repository.allQrRequests) { user, requests ->
        if (user == null || user.role == "MASTER" || user.role == "ADMIN") requests
        else requests.filter { it.bdoId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bdoTodayTarget: StateFlow<TargetEntity?> = combine(_currentUser, repository.allTargets) { user, targets ->
        val today = DateUtils.getTodayDateString()
        if (user == null) null
        else targets.find { it.bdoId == user.id && it.dateString == today }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ASM filtered data
    val asmTls: StateFlow<List<UserEntity>> = combine(_currentUser, repository.allUsers) { user, users ->
        if (user == null || user.role != "ASM") emptyList()
        else users.filter { it.role == "TL" && it.asmId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val asmBdos: StateFlow<List<UserEntity>> = combine(_currentUser, repository.allUsers) { user, users ->
        if (user == null || user.role != "ASM") emptyList()
        else users.filter { it.role == "BDO" && it.asmId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val asmMerchants: StateFlow<List<MerchantEntity>> = combine(_currentUser, repository.allMerchants) { user, merchants ->
        if (user == null || user.role != "ASM") emptyList()
        else merchants.filter { it.asmId == user.id || (user.regionId != null && it.regionId == user.regionId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val asmVisits: StateFlow<List<VisitEntity>> = combine(_currentUser, repository.allVisits) { user, visits ->
        if (user == null || user.role != "ASM") emptyList()
        else visits.filter { it.asmId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val asmQrRequests: StateFlow<List<QrRequestEntity>> = combine(_currentUser, repository.allQrRequests) { user, requests ->
        if (user == null || user.role != "ASM") emptyList()
        else requests.filter { it.asmId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // TL filtered data
    val tlBdos: StateFlow<List<UserEntity>> = combine(_currentUser, repository.allUsers) { user, users ->
        if (user == null || user.role != "TL") emptyList()
        else users.filter { it.role == "BDO" && it.tlId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tlMerchants: StateFlow<List<MerchantEntity>> = combine(_currentUser, repository.allMerchants) { user, merchants ->
        if (user == null || user.role != "TL") emptyList()
        else merchants.filter { it.tlId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tlVisits: StateFlow<List<VisitEntity>> = combine(_currentUser, repository.allVisits) { user, visits ->
        if (user == null || user.role != "TL") emptyList()
        else visits.filter { it.tlId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tlQrRequests: StateFlow<List<QrRequestEntity>> = combine(_currentUser, repository.allQrRequests) { user, requests ->
        if (user == null || user.role != "TL") emptyList()
        else requests.filter { it.tlId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOfflineMode: StateFlow<Boolean> = combine(_isOnline) { onlineArr ->
        !onlineArr[0]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val unsyncedVisitsCount: StateFlow<Int> = combine(unsyncedVisits) { arr ->
        arr[0].size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _gpsThreshold = MutableStateFlow(100.0)
    val gpsThreshold: StateFlow<Double> = _gpsThreshold.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfNeeded()
            _gpsThreshold.value = repository.getGpsThresholdMeters()
        }
    }

    fun login(username: String, plainPass: String) {
        viewModelScope.launch {
            _loginError.value = null
            val (user, err) = repository.login(username, plainPass)
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
                _masterTab.value = MasterTab.DASHBOARD
                _adminTab.value = AdminTab.DASHBOARD
                _asmTab.value = AsmTab.DASHBOARD
                _tlTab.value = TlTab.DASHBOARD
                _bdoTab.value = BdoTab.HOME
            } else {
                _loginError.value = err ?: "Invalid username or password. Please check your credentials."
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeMerchantForVisit.value = null
        _selectedMerchantForDetail.value = null
        _selectedVisitForDetail.value = null
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
        _masterTab.value = MasterTab.DASHBOARD
        _adminTab.value = AdminTab.DASHBOARD
        _asmTab.value = AsmTab.DASHBOARD
        _tlTab.value = TlTab.DASHBOARD
        _bdoTab.value = BdoTab.HOME
    }

    fun setMasterTab(tab: MasterTab) { _masterTab.value = tab }
    fun setAdminTab(tab: AdminTab) { _adminTab.value = tab }
    fun setAsmTab(tab: AsmTab) { _asmTab.value = tab }
    fun setTlTab(tab: TlTab) { _tlTab.value = tab }
    fun setBdoTab(tab: BdoTab) { _bdoTab.value = tab }

    fun startVisit(merchant: MerchantEntity) {
        _activeMerchantForVisit.value = merchant
    }

    fun startVisitForMerchant(merchant: MerchantEntity?) {
        _activeMerchantForVisit.value = merchant
    }

    fun cancelVisit() {
        _activeMerchantForVisit.value = null
    }

    fun viewMerchantDetail(merchant: MerchantEntity?) {
        _selectedMerchantForDetail.value = merchant
    }

    fun viewVisitDetail(visit: VisitEntity?) {
        _selectedVisitForDetail.value = visit
    }

    fun toggleOfflineMode(offline: Boolean) {
        _isOnline.value = !offline
        if (!offline) {
            syncOfflineVisits()
        }
    }

    fun syncOfflineVisits() {
        viewModelScope.launch {
            val count = repository.syncOfflineVisits()
            if (count > 0) {
                _syncMessage.value = "Successfully synchronized $count offline visit(s)."
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun submitVisit(
        visitType: String,
        visitLatitude: Double,
        visitLongitude: Double,
        gpsAccuracy: Float,
        photoPath: String,
        qrPhotoPath: String,
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
        onSuccess: (VisitEntity) -> Unit
    ) {
        val merchant = _activeMerchantForVisit.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            val visit = repository.submitVisit(
                merchant = merchant,
                bdoUser = user,
                visitType = visitType,
                visitLatitude = visitLatitude,
                visitLongitude = visitLongitude,
                gpsAccuracy = gpsAccuracy,
                photoPath = photoPath,
                qrPhotoPath = qrPhotoPath,
                qrDeployed = qrDeployed,
                qrNotDeployedReason = qrNotDeployedReason,
                merchantStatus = merchantStatus,
                paymentDiscussion = paymentDiscussion,
                merchantResponse = merchantResponse,
                followUpRequired = followUpRequired,
                followUpDate = followUpDate,
                comments = comments,
                transactionAmount = transactionAmount,
                qrPhysicalReceived = qrPhysicalReceived,
                qrReceivedDate = qrReceivedDate,
                justificationReason = justificationReason,
                isOnline = _isOnline.value
            )
            _activeMerchantForVisit.value = null
            onSuccess(visit)
        }
    }

    fun submitFieldVisit(
        merchantId: String,
        visitType: String,
        lat: Double,
        lon: Double,
        accuracy: Float,
        photoPath: String,
        qrDeployed: Boolean,
        qrNotDeployedReason: String,
        merchantStatus: String,
        paymentDiscussion: Boolean,
        merchantResponse: String,
        comments: String,
        followUpRequired: Boolean,
        followUpDate: String,
        transactionAmount: Double = 0.0,
        qrPhysicalReceived: Boolean = false,
        qrReceivedDate: String = "",
        justificationReason: String = ""
    ) {
        val merchant = _activeMerchantForVisit.value ?: allMerchants.value.find { it.merchantId == merchantId } ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            repository.submitVisit(
                merchant = merchant,
                bdoUser = user,
                visitType = visitType,
                visitLatitude = lat,
                visitLongitude = lon,
                gpsAccuracy = accuracy,
                photoPath = photoPath,
                qrPhotoPath = "",
                qrDeployed = qrDeployed,
                qrNotDeployedReason = qrNotDeployedReason,
                merchantStatus = merchantStatus,
                paymentDiscussion = paymentDiscussion,
                merchantResponse = merchantResponse,
                followUpRequired = followUpRequired,
                followUpDate = followUpDate,
                comments = comments,
                transactionAmount = transactionAmount,
                qrPhysicalReceived = qrPhysicalReceived,
                qrReceivedDate = qrReceivedDate,
                justificationReason = justificationReason,
                isOnline = _isOnline.value
            )
            _activeMerchantForVisit.value = null
        }
    }

    fun updateVisitVerification(visitId: Long, status: String, notes: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateVisitVerification(visitId, status, notes, admin)
        }
    }

    fun reassignMerchant(merchantId: Long, bdoId: Long, tlId: Long, regionId: Long) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.reassignMerchant(merchantId, bdoId, tlId, regionId, admin)
        }
    }

    fun importMerchants(merchants: List<MerchantEntity>, onComplete: () -> Unit) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.importMerchantsUpsert(merchants, admin)
            onComplete()
        }
    }

    fun upsertTarget(target: TargetEntity) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.upsertTarget(target, admin)
        }
    }

    fun setGpsThreshold(meters: Double) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.setGpsThresholdMeters(meters, admin)
            _gpsThreshold.value = meters
        }
    }

    fun createRegion(name: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createRegion(name, admin)
        }
    }

    fun createTl(name: String, regionId: Long) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createTl(name, regionId, admin)
        }
    }

    fun createBdo(username: String, name: String, mobile: String, regionId: Long, tlId: Long) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val passHash = SecurityUtils.hashPassword("Bdo@12345")
            val tl = allUsers.value.find { it.id == tlId }
            val asmId = tl?.asmId
            val newUser = UserEntity(
                username = username.trim().lowercase(),
                passwordHash = passHash,
                role = "BDO",
                name = name.trim(),
                mobile = mobile.trim(),
                regionId = regionId,
                tlId = tlId,
                asmId = asmId,
                status = "Active"
            )
            repository.createUser(newUser, admin)
        }
    }

    fun createUser(user: UserEntity, onSuccess: (UserEntity) -> Unit = {}) {
        val admin = _currentUser.value
        if (admin == null) {
            _userActionMessage.value = "Error: Please sign in as Master to create users."
            return
        }
        viewModelScope.launch {
            try {
                val newId = repository.createUser(user, admin)
                val created = user.copy(id = newId)
                _userActionMessage.value = "Account for ${user.name} (${user.username}) created successfully!"
                onSuccess(created)
            } catch (e: Exception) {
                _userActionMessage.value = "Error creating account: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun updateUserStatus(userId: Long, newStatus: String) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserStatus(userId, newStatus, admin)
        }
    }

    fun reassignUserHierarchy(userId: Long, tlId: Long?, asmId: Long?, regionId: Long?) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.reassignUserHierarchy(userId, tlId, asmId, regionId, admin)
        }
    }

    fun resetUserPassword(userId: Long, newPlainPass: String, onSuccess: () -> Unit = {}) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            val newHash = SecurityUtils.hashPassword(newPlainPass)
            repository.resetUserPassword(userId, newHash, admin)
            onSuccess()
        }
    }

    fun deleteUser(userId: Long, onSuccess: () -> Unit = {}) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteUser(userId, admin)
            onSuccess()
        }
    }

    fun updateUser(user: UserEntity, onSuccess: () -> Unit = {}) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUser(user, admin)
            onSuccess()
        }
    }

    fun createQrRequest(request: QrRequestEntity, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createQrRequest(request, user)
            onSuccess()
        }
    }

    fun forwardQrToVendor(request: QrRequestEntity, vendorDate: String, vendorRef: String, coordinatorName: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.forwardQrToVendor(request, vendorDate, vendorRef, coordinatorName, user)
        }
    }

    fun updateQrScanning(request: QrRequestEntity, scanningStatus: String, discrepancy: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateQrScanning(request, scanningStatus, discrepancy, user)
        }
    }

    fun updateQrCourier(request: QrRequestEntity, courierName: String, trackingNumber: String, dispatchDate: String, expectedDeliveryDate: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateQrCourier(request, courierName, trackingNumber, dispatchDate, expectedDeliveryDate, user)
        }
    }

    fun confirmQrDelivery(request: QrRequestEntity, actualDeliveryDate: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.confirmQrDelivery(request, actualDeliveryDate, user)
        }
    }

    fun escalateQrRequest(request: QrRequestEntity, escalationComments: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.escalateQrRequest(request, escalationComments, user)
        }
    }

    fun submitNonDeploymentJustification(request: QrRequestEntity, justification: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.submitNonDeploymentJustification(request, justification, user)
        }
    }

    fun createMerchant(merchant: MerchantEntity) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createMerchant(merchant, admin)
        }
    }

    fun updateMerchant(merchant: MerchantEntity) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateMerchant(merchant, admin)
        }
    }

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _lastBackupMessage = MutableStateFlow<String?>(null)
    val lastBackupMessage: StateFlow<String?> = _lastBackupMessage.asStateFlow()

    fun backupToGoogleDrive(context: Context, accessToken: String, onComplete: (Boolean, String) -> Unit) {
        val user = _currentUser.value
        _isBackingUp.value = true
        viewModelScope.launch {
            try {
                val result = com.example.util.GoogleDriveSyncService.performFullBackupToDrive(
                    context = context,
                    db = repository.database,
                    accessToken = accessToken
                )
                _isBackingUp.value = false
                _lastBackupMessage.value = result.message
                if (result.success && user != null) {
                    repository.insertAuditLog(
                        user = user,
                        action = "GOOGLE_DRIVE_BACKUP",
                        entityType = "SYSTEM",
                        entityId = "GDRIVE_BACKUP",
                        recordAffected = "Room Database & Merchant CSV",
                        prevVal = "",
                        newVal = "Uploaded to QR_Merchant_Backups",
                        metadata = "FileId: ${result.fileId ?: "N/A"}"
                    )
                }
                onComplete(result.success, result.message)
            } catch (e: Exception) {
                _isBackingUp.value = false
                val err = "Backup failed: ${e.message}"
                _lastBackupMessage.value = err
                onComplete(false, err)
            }
        }
    }

    fun exportLocalDatabase(context: Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = com.example.util.DatabaseBackupManager.exportDatabaseFile(context, repository.database)
                onComplete(true, "Database snapshot saved to: ${file.name} (${file.length() / 1024} KB)")
            } catch (e: Exception) {
                onComplete(false, "Export failed: ${e.message}")
            }
        }
    }

    fun restoreLocalDatabase(context: Context, backupUri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = com.example.util.DatabaseBackupManager.restoreDatabaseFromFile(context, backupUri)
                if (success) {
                    onComplete(true, "Database restored successfully from backup file!")
                } else {
                    onComplete(false, "Failed to restore database from selected file.")
                }
            } catch (e: Exception) {
                onComplete(false, "Restore error: ${e.message}")
            }
        }
    }
}

class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
