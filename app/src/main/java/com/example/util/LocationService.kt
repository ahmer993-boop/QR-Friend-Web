package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Enterprise LocationService for BDO Real-Time GPS Telemetry.
 * Enforces:
 * 1. High Accuracy FusedLocationProviderClient updates.
 * 2. Mobile Hardware Anti-Mock / Spoofing Detection.
 * 3. Freshness validation (rejects stale cached fixes > 30s).
 * 4. Accuracy radius gating (<= 30 meters).
 */
class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationService"
        const val MAX_ACCURACY_RADIUS_METERS = 30.0f
        const val FRESHNESS_THRESHOLD_MS = 30_000L // 30 seconds max age
    }

    /**
     * Inspects a location fix to ensure it is authentic, highly accurate, and fresh.
     * Returns null if mock GPS, stale cache, or inaccurate radius is detected.
     */
    fun validateLocationTelemetry(location: Location?): Location? {
        if (location == null) return null

        // 1. Detect Mock Locations (Android 12+ API 31+ / Legacy)
        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
        if (isMock) {
            Log.w(TAG, "Mock/Spoofed location provider detected. Dropping ping.")
            return null
        }

        // 2. Enforce High Accuracy Radius Threshold (<= 30 meters)
        if (!location.hasAccuracy() || location.accuracy > MAX_ACCURACY_RADIUS_METERS) {
            Log.w(TAG, "Inaccurate GPS fix dropped: accuracy=${location.accuracy}m (Threshold: 30m)")
            return null
        }

        // 3. Stale Cached Signal Rejection (Age <= 30 seconds from device clock)
        val locationAgeMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        } else {
            System.currentTimeMillis() - location.time
        }
        if (locationAgeMs > FRESHNESS_THRESHOLD_MS) {
            Log.w(TAG, "Stale cached fix dropped ($locationAgeMs ms old). Threshold: $FRESHNESS_THRESHOLD_MS ms")
            return null
        }

        return location
    }

    /**
     * Captures a single validated, high-accuracy location fix.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Try last known location first if authentic and fresh
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLoc: Location? ->
                        val validated = validateLocationTelemetry(lastLoc)
                        if (validated != null) {
                            Log.d(TAG, "Using verified fresh lastLocation: ${validated.latitude}, ${validated.longitude}, acc=${validated.accuracy}m")
                            if (continuation.isActive) continuation.resume(validated)
                        } else {
                            // Request a fresh high-accuracy location fix
                            val locationRequest = LocationRequest.Builder(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                5000L
                            ).apply {
                                setMinUpdateIntervalMillis(2000L)
                                setMaxUpdateDelayMillis(10000L)
                                setMaxUpdates(1)
                            }.build()

                            val callback = object : LocationCallback() {
                                override fun onLocationResult(result: LocationResult) {
                                    val candidate = result.lastLocation ?: lastLoc
                                    val validFix = validateLocationTelemetry(candidate)
                                    Log.d(TAG, "FusedLocationProvider fix obtained: ${validFix?.latitude}, ${validFix?.longitude}, acc=${validFix?.accuracy}m")
                                    fusedLocationClient.removeLocationUpdates(this)
                                    if (continuation.isActive) {
                                        continuation.resume(validFix)
                                    }
                                }
                            }

                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                callback,
                                Looper.getMainLooper()
                            ).addOnFailureListener { exc ->
                                Log.e(TAG, "requestLocationUpdates failed", exc)
                                fusedLocationClient.removeLocationUpdates(callback)
                                if (continuation.isActive) continuation.resume(null)
                            }

                            continuation.invokeOnCancellation {
                                fusedLocationClient.removeLocationUpdates(callback)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get lastLocation", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "Location permission missing", e)
                if (continuation.isActive) continuation.resume(null)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error retrieving location", e)
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    /**
     * Continuous stream of validated, high-accuracy GPS pings for live duty tracking.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 15000L): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).apply {
            setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(10))
            setMinUpdateDistanceMeters(10.0f) // Minimum 10 meters displacement
            setMaxUpdateDelayMillis(TimeUnit.SECONDS.toMillis(30))
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { rawLoc ->
                    val validLoc = validateLocationTelemetry(rawLoc)
                    if (validLoc != null) {
                        trySend(validLoc)
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
