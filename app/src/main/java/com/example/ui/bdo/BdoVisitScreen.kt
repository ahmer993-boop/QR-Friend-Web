package com.example.ui.bdo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.*
import com.example.util.DateUtils
import com.example.util.GeoUtils
import com.example.util.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BdoVisitScreen(
    merchant: MerchantEntity,
    bdoUser: UserEntity,
    gpsThreshold: Double,
    onBack: () -> Unit,
    onSubmitVisit: (
        merchantId: String,
        visitType: String,
        latitude: Double,
        longitude: Double,
        gpsAccuracy: Float,
        photoPath: String,
        qrDeployed: Boolean,
        qrNotDeployedReason: String,
        merchantStatus: String,
        paymentDiscussion: Boolean,
        merchantResponse: String,
        comments: String,
        followUpRequired: Boolean,
        followUpDate: String
    ) -> Unit
) {
    val context = LocalContext.current

    // Real GPS States
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showLocationRationaleDialog by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var isAcquiringGps by remember { mutableStateOf(false) }
    var locationCaptured by remember { mutableStateOf(false) }

    var currentLat by remember { mutableDoubleStateOf(0.0) }
    var currentLon by remember { mutableDoubleStateOf(0.0) }
    var currentAccuracy by remember { mutableFloatStateOf(0.0f) }
    var locationCaptureTime by remember { mutableStateOf("") }

    // Real Camera States
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var cameraActionTarget by remember { mutableStateOf<String?>(null) } // "SHOP" or "QR"

    var shopPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var shopPhotoPath by remember { mutableStateOf("") }
    var qrPhotoPath by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }

    // Function to acquire real device location using FusedLocationProviderClient
    fun fetchRealDeviceLocation() {
        isAcquiringGps = true
        locationError = null
        coroutineScope.launch {
            try {
                val loc = locationService.getCurrentLocation()
                if (loc != null) {
                    currentLat = loc.latitude
                    currentLon = loc.longitude
                    currentAccuracy = loc.accuracy
                    locationCaptureTime = DateUtils.formatTime(loc.time)
                    locationCaptured = true
                    isAcquiringGps = false
                } else {
                    // Fallback to LocationManager if Google Play Services fails
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
                    val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

                    if (!isGpsEnabled && !isNetworkEnabled) {
                        locationError = "Device GPS / Location is turned off. Please enable device location."
                        isAcquiringGps = false
                        return@launch
                    }

                    var fallbackLoc: Location? = null
                    if (isGpsEnabled) {
                        fallbackLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    }
                    if (fallbackLoc == null && isNetworkEnabled) {
                        fallbackLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    }

                    if (fallbackLoc != null) {
                        currentLat = fallbackLoc.latitude
                        currentLon = fallbackLoc.longitude
                        currentAccuracy = fallbackLoc.accuracy
                        locationCaptureTime = DateUtils.formatTime(fallbackLoc.time)
                        locationCaptured = true
                        isAcquiringGps = false
                    } else {
                        val listener = object : LocationListener {
                            override fun onLocationChanged(newLoc: Location) {
                                currentLat = newLoc.latitude
                                currentLon = newLoc.longitude
                                currentAccuracy = newLoc.accuracy
                                locationCaptureTime = DateUtils.formatTime(newLoc.time)
                                locationCaptured = true
                                isAcquiringGps = false
                                locationManager?.removeUpdates(this)
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        }
                        val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                        locationManager?.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    }
                }
            } catch (e: SecurityException) {
                locationError = "Location access denied: ${e.localizedMessage}"
                isAcquiringGps = false
            } catch (e: Exception) {
                locationError = "Could not capture GPS fix: ${e.localizedMessage}"
                isAcquiringGps = false
            }
        }
    }

    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fine || coarse
        if (hasLocationPermission) {
            fetchRealDeviceLocation()
        } else {
            locationError = "Location permission is required to capture the real-time location of this merchant visit."
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Camera Capture Launchers
    val takeShopPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            shopPhotoBitmap = bitmap
            shopPhotoPath = "shop_${merchant.merchantId}_${System.currentTimeMillis()}.jpg"
        }
    }

    val takeQrPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            qrPhotoBitmap = bitmap
            qrPhotoPath = "qr_${merchant.merchantId}_${System.currentTimeMillis()}.jpg"
        }
    }

    // On visit start, initiate location permission flow
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            fetchRealDeviceLocation()
        } else {
            showLocationRationaleDialog = true
        }
    }

    val distanceMeters = if (locationCaptured) {
        GeoUtils.calculateHaversineDistance(
            currentLat, currentLon,
            merchant.latitude, merchant.longitude
        )
    } else 0.0

    val isGpsValid = locationCaptured && (distanceMeters <= gpsThreshold)

    // Form fields
    var selectedVisitType by remember { mutableStateOf("Follow-up Visit") }
    var qrDeployed by remember { mutableStateOf(merchant.qrStatus == "Deployed" || merchant.qrStatus == "Active") }
    var qrNotDeployedReason by remember { mutableStateOf("Merchant Refused") }
    var selectedMerchantStatus by remember { mutableStateOf(merchant.merchantStatus) }
    var paymentDiscussion by remember { mutableStateOf(true) }
    var selectedMerchantResponse by remember { mutableStateOf("Interested") }
    var comments by remember { mutableStateOf("") }
    var followUpRequired by remember { mutableStateOf(false) }
    var followUpDate by remember { mutableStateOf(DateUtils.getTodayDateString()) }

    var showConfirmDialog by remember { mutableStateOf(false) }

    val visitTypeOptions = listOf(
        "Follow-up Visit",
        "New Onboarding",
        "QR Deployment",
        "QR Activation",
        "Merchant Reactivation",
        "QR Replacement",
        "Payment Frequency Follow-up"
    )

    val nonDeploymentReasons = listOf(
        "Merchant Refused",
        "Damaged QR",
        "Shop Closed",
        "Technical Issue",
        "Out of Standees",
        "Already Deployed"
    )

    val merchantStatusOptions = listOf(
        "Active",
        "Inactive",
        "New",
        "Temporarily Closed",
        "Permanently Closed",
        "Refused"
    )

    val merchantResponseOptions = listOf(
        "Interested",
        "Activated",
        "Already Using QR",
        "Needs Follow-up",
        "Refused",
        "Not Available"
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Record Field Visit",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextHeadings
                                )
                            )
                            Text(
                                text = merchant.shopName,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextHeadings)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CardWhite)
                )
                HorizontalDivider(color = BorderColor)
            }
        },
        containerColor = ScreenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ScreenBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("bdo_visit_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Merchant Info Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = merchant.shopName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                    )
                    Text(
                        text = "Merchant ID: ${merchant.merchantId} • Category: ${merchant.merchantCategory}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    Text(
                        text = "Address: ${merchant.address}, ${merchant.city}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                    )
                    Text(
                        text = "Registered Coordinates: ${GeoUtils.formatCoordinates(merchant.latitude, merchant.longitude)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrandGreenDark, fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Real-Time GPS Location Verification Card (Requirement 14)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !locationCaptured -> CardWhite
                        isGpsValid -> SuccessBg
                        else -> DangerBg
                    }
                ),
                border = when {
                    !locationCaptured -> BorderStroke(1.dp, BorderColor)
                    isGpsValid -> BorderStroke(1.dp, Color(0xFF86EFAC))
                    else -> BorderStroke(1.dp, Color(0xFFFCA5A5))
                },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    !locationCaptured -> Icons.Default.MyLocation
                                    isGpsValid -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = when {
                                    !locationCaptured -> TextMuted
                                    isGpsValid -> SuccessGreen
                                    else -> DangerRed
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !locationCaptured -> "GPS ACQUISITION"
                                    isGpsValid -> "GPS VERIFIED"
                                    else -> "GPS MISMATCH"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        !locationCaptured -> TextHeadings
                                        isGpsValid -> SuccessText
                                        else -> DangerText
                                    }
                                )
                            )
                        }

                        if (locationCaptured) {
                            Surface(
                                color = CardWhite,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isGpsValid) Color(0xFF86EFAC) else Color(0xFFFCA5A5))
                            ) {
                                Text(
                                    text = "Distance: ${GeoUtils.formatDistance(distanceMeters)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGpsValid) SuccessText else DangerText
                                    )
                                )
                            }
                        }
                    }

                    if (locationCaptured) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Current Location: Captured via Device GPS",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                            )
                            Text(
                                text = "Latitude: ${String.format("%.6f", currentLat)} | Longitude: ${String.format("%.6f", currentLon)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                            )
                            Text(
                                text = "Accuracy: ±${currentAccuracy.toInt()}m | Capture Time: $locationCaptureTime",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }

                        if (!isGpsValid) {
                            Text(
                                text = "Distance exceeds the ${gpsThreshold.toInt()}m threshold. This visit will be automatically flagged for administrative review.",
                                style = MaterialTheme.typography.labelSmall.copy(color = DangerText, fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        if (locationError != null) {
                            Text(
                                text = locationError!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = DangerText, fontWeight = FontWeight.Medium)
                            )
                        } else {
                            Text(
                                text = "Acquiring real-time device GPS coordinates to verify physical presence at shop...",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    // GPS Trigger Button
                    OutlinedButton(
                        onClick = {
                            if (hasLocationPermission) {
                                fetchRealDeviceLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ButtonSecondaryBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonSecondaryText),
                        enabled = !isAcquiringGps
                    ) {
                        if (isAcquiringGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BrandGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Capturing GPS Fix...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (locationCaptured) "Refresh Real GPS Fix" else "Capture Real Device Location",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Real Camera Access & Photos Section (Requirement 15)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Site Photo Verification (Camera Required)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Shopfront Photo Slot
                        Surface(
                            color = if (shopPhotoBitmap != null) SuccessBg else ScreenBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (shopPhotoBitmap != null) Color(0xFF86EFAC) else BorderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                                .clickable {
                                    if (hasCameraPermission) {
                                        takeShopPhotoLauncher.launch(null)
                                    } else {
                                        cameraActionTarget = "SHOP"
                                        showCameraRationaleDialog = true
                                    }
                                }
                        ) {
                            if (shopPhotoBitmap != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = shopPhotoBitmap!!.asImageBitmap(),
                                        contentDescription = "Shop Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Retake Shopfront",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                                            modifier = Modifier.padding(4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Shopfront Photo *",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                                    )
                                    Text(
                                        text = "Tap to open camera",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }

                        // QR Standee Photo Slot
                        Surface(
                            color = if (qrPhotoBitmap != null) SuccessBg else ScreenBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (qrPhotoBitmap != null) Color(0xFF86EFAC) else BorderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                                .clickable {
                                    if (hasCameraPermission) {
                                        takeQrPhotoLauncher.launch(null)
                                    } else {
                                        cameraActionTarget = "QR"
                                        showCameraRationaleDialog = true
                                    }
                                }
                        ) {
                            if (qrPhotoBitmap != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = qrPhotoBitmap!!.asImageBitmap(),
                                        contentDescription = "QR Standee Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Retake QR Standee",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                                            modifier = Modifier.padding(4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = if (qrDeployed) BrandGreen else TextMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "QR Standee Photo",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                                    )
                                    Text(
                                        text = if (qrDeployed) "Tap to capture QR" else "Optional if refused",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Visit Questionnaire
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Field Questionnaire",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                    )

                    // Visit Type Dropdown
                    var typeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedVisitType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Visit Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                focusedLabelColor = BrandGreenDark,
                                cursorColor = BrandGreen
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            modifier = Modifier.background(CardWhite)
                        ) {
                            visitTypeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = TextPrimary) },
                                    onClick = {
                                        selectedVisitType = option
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // QR Deployment Status
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Was a QR code deployed or already active on site?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextHeadings)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { qrDeployed = true }
                            ) {
                                RadioButton(
                                    selected = qrDeployed,
                                    onClick = { qrDeployed = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandGreen)
                                )
                                Text("Yes (Deployed / Active)", color = TextPrimary)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { qrDeployed = false }
                            ) {
                                RadioButton(
                                    selected = !qrDeployed,
                                    onClick = { qrDeployed = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandGreen)
                                )
                                Text("No", color = TextPrimary)
                            }
                        }
                    }

                    if (!qrDeployed) {
                        var reasonExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = reasonExpanded,
                            onExpandedChange = { reasonExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = qrNotDeployedReason,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Reason for Non-Deployment") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandGreen,
                                    focusedLabelColor = BrandGreenDark,
                                    cursorColor = BrandGreen
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = reasonExpanded,
                                onDismissRequest = { reasonExpanded = false },
                                modifier = Modifier.background(CardWhite)
                            ) {
                                nonDeploymentReasons.forEach { reason ->
                                    DropdownMenuItem(
                                        text = { Text(reason, color = TextPrimary) },
                                        onClick = {
                                            qrNotDeployedReason = reason
                                            reasonExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Merchant Status
                    var statusExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMerchantStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Merchant Operational Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                focusedLabelColor = BrandGreenDark,
                                cursorColor = BrandGreen
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false },
                            modifier = Modifier.background(CardWhite)
                        ) {
                            merchantStatusOptions.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st, color = TextPrimary) },
                                    onClick = {
                                        selectedMerchantStatus = st
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Payment Discussion Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Discussed QR Payment Advantages?",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextHeadings)
                            )
                            Text(
                                text = "Promoted instant bank settlements, zero setup fee, and higher customer conversion.",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                        Switch(
                            checked = paymentDiscussion,
                            onCheckedChange = { paymentDiscussion = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = BorderColor
                            )
                        )
                    }

                    // Merchant Response
                    var responseExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = responseExpanded,
                        onExpandedChange = { responseExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMerchantResponse,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Merchant Response / Sentiment") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = responseExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                focusedLabelColor = BrandGreenDark,
                                cursorColor = BrandGreen
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = responseExpanded,
                            onDismissRequest = { responseExpanded = false },
                            modifier = Modifier.background(CardWhite)
                        ) {
                            merchantResponseOptions.forEach { resp ->
                                DropdownMenuItem(
                                    text = { Text(resp, color = TextPrimary) },
                                    onClick = {
                                        selectedMerchantResponse = resp
                                        responseExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Comments
                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        label = { Text("Visit Notes & Field Observations") },
                        placeholder = { Text("e.g. Shopkeeper agreed to promote QR standee on main counter...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("visit_comments_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            focusedLabelColor = BrandGreenDark,
                            cursorColor = BrandGreen
                        )
                    )

                    // Follow up
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Schedule Follow-up Visit?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                        )
                        Switch(
                            checked = followUpRequired,
                            onCheckedChange = { followUpRequired = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = BorderColor
                            )
                        )
                    }
                }
            }

            // Lock & Verification Notice
            Surface(
                color = BrandGreenLight,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submitted visits are locked and logged in the immutable audit trail.",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrandGreenDark)
                    )
                }
            }

            // Submit Button
            val canSubmit = locationCaptured && (shopPhotoBitmap != null)

            Button(
                onClick = { showConfirmDialog = true },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_visit_final_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGpsValid) ButtonPrimaryBg else WarningOrange,
                    contentColor = ButtonPrimaryText
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        !locationCaptured -> "Capture GPS Location to Proceed"
                        shopPhotoBitmap == null -> "Capture Shop Photo to Proceed"
                        isGpsValid -> "Submit Verified Visit"
                        else -> "Submit Visit (Needs Review)"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // LOCATION PERMISSION RATIONALE DIALOG (Requirement 14)
    if (showLocationRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showLocationRationaleDialog = false },
            containerColor = CardWhite,
            title = {
                Text(
                    text = "Location Access Required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                )
            },
            text = {
                Text(
                    text = "QR Friend needs your location to capture the real-time location of this merchant visit.\n\nReal device GPS coordinates are required to verify physical presence at the merchant's store.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationRationaleDialog = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimaryBg,
                        contentColor = ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLocationRationaleDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ButtonSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonSecondaryText)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // CAMERA PERMISSION RATIONALE DIALOG (Requirement 15)
    if (showCameraRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraRationaleDialog = false },
            containerColor = CardWhite,
            title = {
                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextHeadings)
                )
            },
            text = {
                Text(
                    text = "QR Friend needs camera access to capture merchant and QR deployment evidence.\n\nReal photos must be taken directly using the device camera.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraRationaleDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimaryBg,
                        contentColor = ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enable Camera")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCameraRationaleDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ButtonSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonSecondaryText)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = CardWhite,
            title = {
                Text("Confirm Visit Submission", color = TextHeadings, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Shop: ${merchant.shopName}", color = TextPrimary)
                    Text("Visit Type: $selectedVisitType", color = TextPrimary)
                    Text("QR Deployed: ${if (qrDeployed) "Yes" else "No ($qrNotDeployedReason)"}", color = TextPrimary)
                    Text(
                        text = "GPS Status: ${if (isGpsValid) "VALID (${GeoUtils.formatDistance(distanceMeters)})" else "MISMATCH (${GeoUtils.formatDistance(distanceMeters)})"}",
                        color = if (isGpsValid) SuccessText else DangerText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Once submitted, this record cannot be edited.",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onSubmitVisit(
                            merchant.merchantId,
                            selectedVisitType,
                            currentLat,
                            currentLon,
                            currentAccuracy,
                            shopPhotoPath,
                            qrDeployed,
                            if (qrDeployed) "" else qrNotDeployedReason,
                            selectedMerchantStatus,
                            paymentDiscussion,
                            selectedMerchantResponse,
                            comments,
                            followUpRequired,
                            followUpDate
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimaryBg,
                        contentColor = ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirm & Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
