package com.example.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates distance between two GPS coordinates using Haversine formula in meters.
     */
    fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Formats distance in meters into human readable string (e.g., "24 m" or "1.8 km")
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000.0) {
            "%.0f m".format(meters)
        } else {
            "%.2f km".format(meters / 1000.0)
        }
    }

    /**
     * Determines whether visit is VALID or GPS MISMATCH based on distance and threshold.
     */
    fun getGpsStatus(distanceMeters: Double, thresholdMeters: Double = 100.0): String {
        return if (distanceMeters <= thresholdMeters) "VALID" else "GPS MISMATCH"
    }

    fun formatCoordinates(lat: Double, lon: Double): String {
        return "%.5f, %.5f".format(lat, lon)
    }
}
