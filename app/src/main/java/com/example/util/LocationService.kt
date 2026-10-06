package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
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
import kotlin.coroutines.resume

/**
 * Production LocationService using Google Play Services FusedLocationProviderClient.
 * Accurately captures and logs high-precision GPS coordinates when a field agent visits a merchant location.
 */
class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationService"
        private const val FRESHNESS_THRESHOLD_MS = 60_000L // 1 minute
    }

    /**
     * Captures the best available current location with HIGH_ACCURACY priority.
     * Checks lastKnownLocation first if fresh, otherwise requests a fresh single update.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Try last known location first for immediate response
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLoc: Location? ->
                        val isFresh = lastLoc != null &&
                                (System.currentTimeMillis() - lastLoc.time) < FRESHNESS_THRESHOLD_MS

                        if (isFresh && lastLoc != null) {
                            Log.d(TAG, "Using fresh lastLocation: ${lastLoc.latitude}, ${lastLoc.longitude}, acc=${lastLoc.accuracy}m")
                            if (continuation.isActive) continuation.resume(lastLoc)
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
                                    val location = result.lastLocation ?: lastLoc
                                    Log.d(TAG, "FusedLocationProvider fix obtained: ${location?.latitude}, ${location?.longitude}, acc=${location?.accuracy}m")
                                    fusedLocationClient.removeLocationUpdates(this)
                                    if (continuation.isActive) {
                                        continuation.resume(location)
                                    }
                                }
                            }

                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                callback,
                                Looper.getMainLooper()
                            ).addOnFailureListener { exc ->
                                Log.e(TAG, "requestLocationUpdates failed, falling back to lastLoc", exc)
                                fusedLocationClient.removeLocationUpdates(callback)
                                if (continuation.isActive) continuation.resume(lastLoc)
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
     * Flow of continuous location updates for live tracking during merchant visits.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 5000L): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).apply {
            setMinUpdateIntervalMillis(intervalMs / 2)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location)
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
