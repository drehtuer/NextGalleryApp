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

    @Test
    fun formatted_northEast_matchesDesign() {
        assertEquals("45.62° N, 10.68° E", GeoPoint(45.6214, 10.6849).formatted())
    }

    @Test
    fun formatted_southWest_usesPositiveNumbers() {
        assertEquals("72.50° S, 40.00° W", GeoPoint(-72.5, -40.0).formatted())
    }
}
