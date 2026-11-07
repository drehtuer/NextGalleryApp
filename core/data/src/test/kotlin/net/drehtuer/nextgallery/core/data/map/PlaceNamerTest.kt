package net.drehtuer.nextgallery.core.data.map

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.TestDispatcherProvider
import net.drehtuer.nextgallery.core.model.Cluster
import net.drehtuer.nextgallery.core.model.ClusterTitle
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The places are made up for these tests; only their positions and sizes matter. */
class PlaceNamerTest {
    private val small = Place("Smalltown", "IT", 45.0, 10.0, 16_000)
    private val large = Place("Bigcity", "IT", 46.0, 11.0, 100_000)
    private var loads = 0

    private fun TestScope.namer(places: List<Place> = listOf(small, large)) = PlaceNamer(
        source = {
            loads++
            places
        },
        dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
    )

    private fun clusterAt(vararg points: GeoPoint) = Cluster(
        points.first(),
        points.mapIndexed { index, point -> GeotaggedImage(MediaId(index.toLong()), point, dateTakenMillis = 0) },
    )

    @Test
    fun nearest_nearPlace_returnsIt() = runTest {
        assertEquals(small, namer().nearest(GeoPoint(45.01, 10.0)))
    }

    @Test
    fun nearest_noPlaceNearby_returnsNull() = runTest {
        assertNull(namer().nearest(GeoPoint(0.0, 0.0)))
    }

    @Test
    fun nearest_noPlaceList_returnsNull() = runTest {
        assertNull(namer(places = emptyList()).nearest(GeoPoint(45.01, 10.0)))
    }

    @Test
    fun nearest_severalPoints_readsTheListOnce() = runTest {
        val namer = namer()

        namer.nearest(GeoPoint(45.01, 10.0))
        namer.nearest(GeoPoint(46.01, 11.0))

        assertEquals(1, loads)
    }

    @Test
    fun nearest_samePointTwice_givesSameAnswer() = runTest {
        val namer = namer()

        namer.nearest(GeoPoint(0.0, 0.0))

        assertNull(namer.nearest(GeoPoint(0.0, 0.0)))
    }

    @Test
    fun clusterTitle_onePlace_isNamedAfterIt() = runTest {
        val title = namer().clusterTitle(clusterAt(GeoPoint(45.01, 10.0), GeoPoint(45.02, 10.0)))

        assertEquals(ClusterTitle(small, morePlaces = 0), title)
    }

    @Test
    fun clusterTitle_mergedCluster_isNamedAfterLargestPlace() = runTest {
        val title = namer().clusterTitle(clusterAt(GeoPoint(45.01, 10.0), GeoPoint(46.01, 11.0)))

        assertEquals(ClusterTitle(large, morePlaces = 1), title)
    }

    @Test
    fun clusterTitle_someImagesWithoutTown_usesThoseWithOne() = runTest {
        val title = namer().clusterTitle(clusterAt(GeoPoint(0.0, 0.0), GeoPoint(45.01, 10.0)))

        assertEquals(ClusterTitle(small, morePlaces = 0), title)
    }

    @Test
    fun clusterTitle_noTown_returnsNull() = runTest {
        assertNull(namer().clusterTitle(clusterAt(GeoPoint(0.0, 0.0))))
    }
}
