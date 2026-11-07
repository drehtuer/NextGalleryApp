package net.drehtuer.nextgallery.core.model

import java.util.Locale
import kotlin.math.abs

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -MAX_LATITUDE..MAX_LATITUDE) { "latitude out of range: $latitude" }
        require(longitude in -MAX_LONGITUDE..MAX_LONGITUDE) { "longitude out of range: $longitude" }
    }

    /** As in DESIGN.md: "45.62° N, 10.68° E". */
    fun formatted(): String {
        val northSouth = if (latitude < 0) "S" else "N"
        val eastWest = if (longitude < 0) "W" else "E"
        return String.format(Locale.ROOT, "%.2f° %s, %.2f° %s", abs(latitude), northSouth, abs(longitude), eastWest)
    }

    private companion object {
        const val MAX_LATITUDE = 90.0
        const val MAX_LONGITUDE = 180.0
    }
}
