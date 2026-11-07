package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ClusterTest {
    private val older = GeotaggedImage(MediaId(1), GeoPoint(45.0, 10.0), dateTakenMillis = 100)
    private val newer = GeotaggedImage(MediaId(2), GeoPoint(46.0, 11.0), dateTakenMillis = 200)
    private val cluster = Cluster(GeoPoint(45.5, 10.5), listOf(older, newer))

    @Test
    fun count_isNumberOfImages() {
        assertEquals(2, cluster.count)
    }

    @Test
    fun coverId_isNewestImage() {
        assertEquals(MediaId(2), cluster.coverId)
    }

    @Test
    fun bounds_surroundAllImages() {
        assertEquals(GeoBounds(south = 45.0, west = 10.0, north = 46.0, east = 11.0), cluster.bounds)
    }
}
