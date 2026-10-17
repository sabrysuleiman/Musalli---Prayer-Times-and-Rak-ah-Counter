package com.example.data

import kotlin.math.*

object QiblaUtil {
    // Kaaba Coordinates
    const val KAABA_LAT = 21.422487
    const val KAABA_LNG = 39.826206

    /**
     * Calculates the Qibla bearing in degrees from true North (0-360)
     */
    fun calculateQiblaBearing(userLat: Double, userLng: Double): Double {
        val lat1 = Math.toRadians(userLat)
        val lng1 = Math.toRadians(userLng)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lng2 = Math.toRadians(KAABA_LNG)

        val deltaLng = lng2 - lng1

        val y = sin(deltaLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360.0) % 360.0
        return bearing
    }

    /**
     * Calculates distance to Mecca in kilometers
     */
    fun calculateDistanceToKaabaKm(userLat: Double, userLng: Double): Int {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - userLat)
        val dLng = Math.toRadians(KAABA_LNG - userLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(userLat)) * cos(Math.toRadians(KAABA_LAT)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadiusKm * c).toInt()
    }
}
