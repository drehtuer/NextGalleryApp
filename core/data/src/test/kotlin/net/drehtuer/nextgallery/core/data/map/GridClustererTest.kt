package net.drehtuer.nextgallery.core.data.map

import net.drehtuer.nextgallery.core.model.Cluster
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GridClustererTest {
    private val world = GeoBounds(south = -90.0, west = -180.0, north = 90.0, east = 180.0)

    private fun image(id: Long, latitude: Double, longitude: Double) =
        GeotaggedImage(MediaId(id), GeoPoint(latitude, longitude), dateTakenMillis = id)

    private fun cluster(points: List<GeotaggedImage>, zoom: Int, bounds: GeoBounds = world): List<Cluster> =
        GridClusterer.cluster(points, bounds, zoom, cellPx = 80)

    @Test
    fun cluster_noPoints_returnsNothing() {
        assertTrue(cluster(emptyList(), zoom = 5).isEmpty())
    }

    @Test
    fun cluster_singlePoint_isItsOwnCluster() {
        val clusters = cluster(listOf(image(1, 45.5, 10.5)), zoom = 5)

        assertEquals(1, clusters.single().count)
    }

    @Test
    fun cluster_singlePoint_isCentredOnIt() {
        val center = cluster(listOf(image(1, 45.5, 10.5)), zoom = 5).single().center

        assertEquals(45.5, center.latitude, 1e-9)
        assertEquals(10.5, center.longitude, 1e-9)
    }

    @Test
    fun cluster_closeTogether_mergeInOneCell() {
        val points = listOf(image(1, 45.50, 10.50), image(2, 45.51, 10.51))

        assertEquals(2, cluster(points, zoom = 4).single().count)
    }

    @Test
    fun cluster_higherZoom_splitsThem() {
        val points = listOf(image(1, 45.50, 10.50), image(2, 45.51, 10.51))

        assertEquals(2, cluster(points, zoom = 18).size)
    }

    @Test
    fun cluster_merged_isCentredBetweenItsImages() {
        val points = listOf(image(1, 45.50, 10.50), image(2, 45.52, 10.52))

        val center = cluster(points, zoom = 4).single().center

        assertEquals(45.51, center.latitude, 1e-6)
        assertEquals(10.51, center.longitude, 1e-6)
    }

    @Test
    fun cluster_outsideBounds_isLeftOut() {
        val bounds = GeoBounds(south = 40.0, west = 0.0, north = 50.0, east = 20.0)
        val points = listOf(image(1, 45.5, 10.5), image(2, -33.9, 151.2))

        assertEquals(listOf(MediaId(1)), cluster(points, zoom = 5, bounds = bounds).single().ids())
    }

    @Test
    fun cluster_boundsCrossingAntimeridian_keepBothSides() {
        val bounds = GeoBounds(south = -30.0, west = 170.0, north = 30.0, east = -170.0)
        val points = listOf(image(1, 0.0, 175.0), image(2, 0.0, -175.0), image(3, 0.0, 0.0))

        assertEquals(2, cluster(points, zoom = 5, bounds = bounds).size)
    }

    @Test
    fun cluster_sameCellAcrossAntimeridian_isCentredAtTheAntimeridian() {
        val points = listOf(image(1, 0.0, 179.0), image(2, 0.0, -179.0))

        val clusters = GridClusterer.cluster(points, world, zoom = 0, cellPx = 512)

        assertEquals(180.0, abs(clusters.single().center.longitude), 1e-6)
    }

    @Test
    fun cluster_nearThePoles_doesNotFail() {
        val points = listOf(image(1, 89.9, 0.0), image(2, -89.9, 0.0))

        assertEquals(2, cluster(points, zoom = 3).size)
    }

    @Test
    fun cluster_atTheEastEdge_staysInTheLastCell() {
        val points = listOf(image(1, 0.0, 180.0))

        assertEquals(1, cluster(points, zoom = 3).single().count)
    }

    private fun Cluster.ids() = items.map { it.id }
}
