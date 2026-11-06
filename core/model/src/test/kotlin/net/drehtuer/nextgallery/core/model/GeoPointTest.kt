package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPointTest {
    @Test
    fun init_validCoordinates_keepsValues() {
        val point = GeoPoint(latitude = 48.1, longitude = 11.5)

        assertEquals(48.1, point.latitude, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun init_latitudeOutOfRange_throws() {
        GeoPoint(latitude = 90.1, longitude = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun init_longitudeOutOfRange_throws() {
        GeoPoint(latitude = 0.0, longitude = -180.1)
    }
}
