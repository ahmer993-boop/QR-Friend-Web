package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Event-Driven Location Capture Manager.
 * Captures GPS coordinates ONLY on:
 * 1. "APP_OPEN" - Foreground / Resume.
 * 2. "VISIT_DISPATCH" - Field visit submission.
 */
class LocationTelemetryManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationTelemetry"
        const val EVENT_APP_OPEN = "APP_OPEN"
        const val EVENT_VISIT_DISPATCH = "VISIT_DISPATCH"
    }

    /**
     * Captures an immediate single GPS location snapshot with HIGH_ACCURACY priority.
     */
    @SuppressLint("MissingPermission")
    suspend fun captureCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        val cts = CancellationTokenSource()
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture single location snapshot: ${e.message}")
            null
        }
    }

    /**
     * Constructs the event payload and dispatches it to the backend /api/locations endpoint.
     */
    suspend fun recordLocationEvent(
        userId: String,
        eventType: String,
        backendUrl: String = "http://192.168.43.122:8080"
    ): Boolean = withContext(Dispatchers.IO) {
        val location = captureCurrentLocation() ?: return@withContext false

        val payload = JSONObject().apply {
            put("userId", userId)
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("eventType", eventType)
            put("timestamp", System.currentTimeMillis())
            put("accuracy", location.accuracy)
        }

        try {
            val url = URL("$backendUrl/api/locations")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-user-id", userId)
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val responseCode = conn.responseCode
            conn.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transmit event location: ${e.message}")
            false
        }
    }
}
