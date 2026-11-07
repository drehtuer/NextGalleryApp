package net.drehtuer.nextgallery.core.data.map

import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The places are made up for these tests; only their positions matter. */
class PlaceIndexTest {
    private val town = Place("Testtown", "IT", 45.0, 10.0, 20_000)

    @Test
    fun nearest_noPlaces_returnsNull() {
        assertNull(PlaceIndex(emptyList()).nearest(GeoPoint(45.0, 10.0)))
    }

    @Test
    fun nearest_within30Km_returnsPlace() {
        // 0.25° of latitude is about 28 km.
        assertEquals(town, PlaceIndex(listOf(town)).nearest(GeoPoint(45.25, 10.0)))
    }

    @Test
    fun nearest_beyond30Km_returnsNull() {
        // 0.3° of latitude is about 33 km.
        assertNull(PlaceIndex(listOf(town)).nearest(GeoPoint(45.3, 10.0)))
    }

    @Test
    fun nearest_twoPlaces_returnsCloserOne() {
        val other = Place("Otherville", "IT", 45.1, 10.0, 500_000)

        assertEquals(other, PlaceIndex(listOf(town, other)).nearest(GeoPoint(45.08, 10.0)))
    }

    @Test
    fun nearest_inNeighbouringCell_isFound() {
        val point = GeoPoint(44.99, 9.99)

        assertEquals(town, PlaceIndex(listOf(town)).nearest(point))
    }

    @Test
    fun nearest_acrossAntimeridian_isFound() {
        val fiji = Place("Islandtown", "FJ", -17.0, 179.95, 20_000)

        assertEquals(fiji, PlaceIndex(listOf(fiji)).nearest(GeoPoint(-17.0, -179.95)))
    }

    @Test
    fun nearest_nearThePole_searchesManyLongitudes() {
        // At 80° north a degree of longitude is only about 19 km, so this is 19 km away but two cells over.
        val arctic = Place("Polartown", "NO", 80.0, 10.5, 20_000)

        assertEquals(arctic, PlaceIndex(listOf(arctic)).nearest(GeoPoint(80.0, 9.5)))
    }

    @Test
    fun nearest_atThePole_doesNotFail() {
        assertNull(PlaceIndex(listOf(town)).nearest(GeoPoint(90.0, 0.0)))
    }

    @Test
    fun nearest_largerRadius_isRespected() {
        assertEquals(town, PlaceIndex(listOf(town)).nearest(GeoPoint(45.5, 10.0), maxKm = 100.0))
    }
}
