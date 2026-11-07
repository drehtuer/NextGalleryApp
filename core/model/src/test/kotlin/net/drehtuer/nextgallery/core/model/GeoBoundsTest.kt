package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoBoundsTest {
    private val garda = GeoBounds(south = 45.0, west = 10.0, north = 46.0, east = 11.0)
    private val pacific = GeoBounds(south = -20.0, west = 170.0, north = 20.0, east = -170.0)

    @Test
    fun contains_pointInside_isTrue() {
        assertTrue(garda.contains(GeoPoint(45.5, 10.5)))
    }

    @Test
    fun contains_pointOnEdge_isTrue() {
        assertTrue(garda.contains(GeoPoint(45.0, 10.0)))
    }

    @Test
    fun contains_latitudeOutside_isFalse() {
        assertFalse(garda.contains(GeoPoint(47.0, 10.5)))
    }

    @Test
    fun contains_longitudeOutside_isFalse() {
        assertFalse(garda.contains(GeoPoint(45.5, 12.0)))
    }

    @Test
    fun contains_crossingAntimeridian_acceptsEastOfWestEdge() {
        assertTrue(pacific.contains(GeoPoint(0.0, 175.0)))
    }

    @Test
    fun contains_crossingAntimeridian_acceptsWestOfEastEdge() {
        assertTrue(pacific.contains(GeoPoint(0.0, -175.0)))
    }

    @Test
    fun contains_crossingAntimeridian_rejectsTheOtherSideOfTheWorld() {
        assertFalse(pacific.contains(GeoPoint(0.0, 0.0)))
    }

    @Test
    fun decode_encodedBounds_returnsSameBounds() {
        val bounds = GeoBounds(45.123456789012, 10.1, 45.2, 10.987654321098)

        assertEquals(bounds, GeoBounds.decode(bounds.encode()))
    }

    @Test
    fun decode_tooFewValues_returnsNull() {
        assertNull(GeoBounds.decode("1.0,2.0,3.0"))
    }

    @Test
    fun decode_notANumber_returnsNull() {
        assertNull(GeoBounds.decode("1.0,2.0,3.0,north"))
    }

    @Test
    fun around_returnsSmallestRectangle() {
        val bounds = GeoBounds.around(listOf(GeoPoint(45.0, 11.0), GeoPoint(46.0, 10.0)))

        assertEquals(GeoBounds(south = 45.0, west = 10.0, north = 46.0, east = 11.0), bounds)
    }
}
