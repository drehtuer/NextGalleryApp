package net.drehtuer.nextgallery.core.data.map

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeGeoIndexStore
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.TestDispatcherProvider
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.model.ClusterTitle
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.Place
import net.drehtuer.nextgallery.core.model.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMapRepositoryTest {
    private val geoStore = FakeGeoIndexStore()
    private val indexStore = FakeMediaIndexStore()
    private val town = Place("Testtown", "IT", 45.5, 10.5, 20_000)
    private val garda = GeoPoint(45.5, 10.5)
    private val viewport = Viewport(garda, zoom = 8.0, GeoBounds(south = 40.0, west = 5.0, north = 50.0, east = 15.0))

    private fun TestScope.repository(): DefaultMapRepository {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        return DefaultMapRepository(geoStore, indexStore, PlaceNamer({ listOf(town) }, dispatchers), dispatchers)
    }

    private fun image(id: Long, point: GeoPoint = garda, date: Long = id) = GeotaggedImage(MediaId(id), point, date)

    @Test
    fun observeClusters_returnsClusterWithTitleAndCover() = runTest {
        val cover = mediaItem(1)
        indexStore.upsertAll(listOf(cover))
        geoStore.geotagged.value = listOf(image(1))

        repository().observeClusters(MutableStateFlow(viewport)).test {
            val cluster = awaitItem().single()

            assertEquals(ClusterTitle(town, morePlaces = 0), cluster.title)
            assertEquals(cover, cluster.cover)
        }
    }

    @Test
    fun observeClusters_coverAlreadyDeleted_hasNoCover() = runTest {
        geoStore.geotagged.value = listOf(image(1))

        repository().observeClusters(MutableStateFlow(viewport)).test {
            assertNull(awaitItem().single().cover)
        }
    }

    @Test
    fun observeClusters_indexChanges_recomputes() = runTest {
        repository().observeClusters(MutableStateFlow(viewport)).test {
            assertTrue(awaitItem().isEmpty())

            geoStore.geotagged.value = listOf(image(1))

            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun observeClusters_viewportChangesQuickly_computesOnlyTheLast() = runTest {
        val elsewhere = viewport.copy(bounds = GeoBounds(south = -10.0, west = -10.0, north = 10.0, east = 10.0))
        geoStore.geotagged.value = listOf(image(1))
        val viewports = MutableStateFlow<Viewport?>(null)

        repository().observeClusters(viewports.filterNotNull()).test {
            viewports.value = viewport
            advanceTimeBy(DefaultMapRepository.VIEWPORT_DEBOUNCE_MILLIS / 2)
            runCurrent()
            viewports.value = elsewhere

            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun observeHasGeotagged_noImages_isFalse() = runTest {
        repository().observeHasGeotagged().test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun observeHasGeotagged_withImages_isTrue() = runTest {
        geoStore.geotagged.value = listOf(image(1))

        repository().observeHasGeotagged().test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun startCamera_locationKnown_centresThereAtZoom8() = runTest {
        assertEquals(MapCamera(garda, 8.0), repository().startCamera(garda))
    }

    @Test
    fun startCamera_locationUnknown_opensOnAreaWithMostImages() = runTest {
        val sydney = GeoPoint(-33.9, 151.2)
        geoStore.geotagged.value = listOf(image(1), image(2, sydney), image(3, sydney))

        val camera = repository().startCamera(null)

        assertEquals(-33.9, camera.center.latitude, 1e-6)
        assertEquals(151.2, camera.center.longitude, 1e-6)
        assertEquals(8.0, camera.zoom, 0.0)
    }

    @Test
    fun startCamera_noLocationAndNoImages_showsTheWorld() = runTest {
        assertEquals(MapCamera(GeoPoint(0.0, 0.0), 2.0), repository().startCamera(null))
    }
}
