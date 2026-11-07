package net.drehtuer.nextgallery.core.data.index

import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.RawExif
import kotlin.math.abs

/** Turns the raw EXIF GPS tags into a position. */
object GpsParser {
    private const val PART_COUNT = 3
    private const val MINUTES_PER_DEGREE = 60.0
    private const val SECONDS_PER_DEGREE = 3600.0
    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0

    /** Returns null if a tag is missing or malformed, and for 0° N, 0° E, which is what redacted files contain. */
    fun parse(exif: RawExif): GeoPoint? {
        val latitude = signedDegrees(exif.gpsLatitude, exif.gpsLatitudeRef, positive = "N", negative = "S")
        val longitude = signedDegrees(exif.gpsLongitude, exif.gpsLongitudeRef, positive = "E", negative = "W")
        return when {
            latitude == null || longitude == null -> null
            abs(latitude) > MAX_LATITUDE || abs(longitude) > MAX_LONGITUDE -> null
            latitude == 0.0 && longitude == 0.0 -> null
            else -> GeoPoint(latitude, longitude)
        }
    }

    private fun signedDegrees(raw: String?, ref: String?, positive: String, negative: String): Double? {
        val degrees = degrees(raw) ?: return null
        return when (ref?.trim()?.uppercase()) {
            positive -> degrees
            negative -> -degrees
            else -> null
        }
    }

    /** EXIF stores degrees, minutes and seconds as fractions: "45/1,37/1,1234/100". */
    private fun degrees(raw: String?): Double? = raw?.split(',')
        ?.mapNotNull(::fraction)
        ?.takeIf { it.size == PART_COUNT }
        ?.let { (degrees, minutes, seconds) -> degrees + minutes / MINUTES_PER_DEGREE + seconds / SECONDS_PER_DEGREE }

    private fun fraction(raw: String): Double? {
        val parts = raw.split('/')
        val numerator = parts[0].trim().toDoubleOrNull() ?: return null
        val denominator = if (parts.size > 1) parts[1].trim().toDoubleOrNull() else 1.0
        return denominator?.takeIf { it != 0.0 }?.let { numerator / it }
    }
}
