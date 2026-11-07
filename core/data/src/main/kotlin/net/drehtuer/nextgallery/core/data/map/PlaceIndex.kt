package net.drehtuer.nextgallery.core.data.map

import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.Place
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Finds the nearest place quickly: places are sorted into cells of one degree, so only a few are compared. */
class PlaceIndex(places: List<Place>) {
    private val cells: Map<Int, List<Place>> =
        places.groupBy { key(latitudeCell(it.latitude), longitudeCell(it.longitude)) }

    /** Returns the nearest place within [maxKm], or null if there is none (D45). */
    fun nearest(point: GeoPoint, maxKm: Double = DEFAULT_MAX_KM): Place? {
        val latitudeReach = ceil(maxKm / KM_PER_DEGREE).toInt()
        val longitudeReach = longitudeReach(point.latitude, latitudeReach, maxKm)
        val centerLatitude = latitudeCell(point.latitude)
        val centerLongitude = longitudeCell(point.longitude)
        var nearest: Place? = null
        var nearestKm = maxKm
        for (latitude in centerLatitude - latitudeReach..centerLatitude + latitudeReach) {
            for (longitude in centerLongitude - longitudeReach..centerLongitude + longitudeReach) {
                cells[key(latitude, longitude.mod(LONGITUDE_CELLS))]?.forEach { place ->
                    val km = distanceKm(point, place)
                    if (km <= nearestKm) {
                        nearest = place
                        nearestKm = km
                    }
                }
            }
        }
        return nearest
    }

    /** A degree of longitude gets shorter towards the poles, so more cells have to be searched there. */
    private fun longitudeReach(latitude: Double, latitudeReach: Int, maxKm: Double): Int {
        val farthestLatitude = min(abs(latitude) + latitudeReach, QUARTER_CIRCLE)
        val kmPerDegree = max(KM_PER_DEGREE * cos(Math.toRadians(farthestLatitude)), MIN_KM_PER_DEGREE)
        return min(ceil(maxKm / kmPerDegree).toInt(), LONGITUDE_CELLS / 2)
    }

    private fun distanceKm(point: GeoPoint, place: Place): Double {
        val latitude1 = Math.toRadians(point.latitude)
        val latitude2 = Math.toRadians(place.latitude)
        val deltaLatitude = latitude2 - latitude1
        val deltaLongitude = Math.toRadians(place.longitude - point.longitude)
        val haversine = sin(deltaLatitude / 2).pow(2) +
            cos(latitude1) * cos(latitude2) * sin(deltaLongitude / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(haversine)))
    }

    private fun latitudeCell(latitude: Double): Int = floor(latitude).toInt()

    private fun longitudeCell(longitude: Double): Int = floor(longitude).toInt().mod(LONGITUDE_CELLS)

    private fun key(latitudeCell: Int, longitudeCell: Int): Int = latitudeCell * LONGITUDE_CELLS + longitudeCell

    companion object {
        const val DEFAULT_MAX_KM = 30.0
        private const val EARTH_RADIUS_KM = 6371.0
        private const val KM_PER_DEGREE = 111.0
        private const val MIN_KM_PER_DEGREE = 0.1
        private const val QUARTER_CIRCLE = 90.0
        private const val LONGITUDE_CELLS = 360
    }
}
