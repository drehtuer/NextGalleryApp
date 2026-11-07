package net.drehtuer.nextgallery.core.data.media

import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeExifSource
import net.drehtuer.nextgallery.core.data.fakes.FakeGeoIndexStore
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.FakeTimelineStore
import net.drehtuer.nextgallery.core.data.fakes.TestDispatcherProvider
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.data.map.PlaceNamer
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.Place
import net.drehtuer.nextgallery.core.model.RawExif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMediaRepositoryTest {
    private val store = FakeMediaIndexStore()
    private val timelineStore = FakeTimelineStore()
    private val folder = MediaSource.Folder(7)

    private val geoStore = FakeGeoIndexStore()
    private val desenzano = Place("Desenzano del Garda", "IT", 45.47, 10.54, 22_665)

    private fun TestScope.repository(exif: RawExif? = null): DefaultMediaRepository {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        val placeNamer = PlaceNamer({ listOf(desenzano) }, dispatchers)
        return DefaultMediaRepository(store, timelineStore, geoStore, FakeExifSource(exif), placeNamer)
    }

    @Test
    fun getItem_unknownId_returnsNull() = runTest {
        assertNull(repository().getItem(MediaId(1)))
    }

    @Test
    fun pagedItems_folder_returnsItemsOfThatFolder() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8)))

        val snapshot = repository().pagedItems(folder).asSnapshot()

        assertEquals(listOf(MediaId(1)), snapshot.map { it.id })
    }

    @Test
    fun pagedItems_virtualFolder_returnsItemsOfItsMembers() = runTest {
        store.upsertAll(
            listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8), mediaItem(id = 3, bucketId = 9)),
        )

        val snapshot = repository().pagedItems(MediaSource.VirtualFolder(listOf(7, 8))).asSnapshot()

        assertEquals(listOf(MediaId(1), MediaId(2)), snapshot.map { it.id })
    }

    @Test
    fun pagedItems_timeline_returnsItemsOfAllFolders() = runTest {
        timelineStore.items = listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8))

        val snapshot = repository().pagedItems(MediaSource.Timeline).asSnapshot()

        assertEquals(listOf(MediaId(1), MediaId(2)), snapshot.map { it.id })
    }

    @Test
    fun pagedItems_usesPlaceholders() = runTest {
        repository().pagedItems(folder)

        assertTrue(store.pagingConfig?.enablePlaceholders == true)
    }

    @Test
    fun pagedItems_jumpsToFarAwayPages() = runTest {
        repository().pagedItems(folder)

        assertEquals(360, store.pagingConfig?.jumpThreshold)
    }

    @Test
    fun indexOf_folder_returnsPositionInFolder() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1), mediaItem(id = 2)))

        assertEquals(1, repository().indexOf(folder, MediaId(2)))
    }

    @Test
    fun indexOf_virtualFolder_returnsPositionInIt() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8)))

        assertEquals(1, repository().indexOf(MediaSource.VirtualFolder(listOf(7, 8)), MediaId(2)))
    }

    @Test
    fun indexOf_timeline_returnsPositionInTimeline() = runTest {
        timelineStore.items = listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8))

        assertEquals(1, repository().indexOf(MediaSource.Timeline, MediaId(2)))
    }

    @Test
    fun timelineItemIds_returnsIdsOfAllFolders() = runTest {
        timelineStore.items = listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8))

        assertEquals(listOf(MediaId(1), MediaId(2)), repository().timelineItemIds())
    }

    @Test
    fun pagedItems_map_readsTheClusterBounds() = runTest {
        val bounds = GeoBounds(45.0, 10.0, 46.0, 11.0)

        repository().pagedItems(MediaSource.Map(bounds))

        assertEquals(listOf(bounds), geoStore.pagedBounds)
    }

    @Test
    fun indexOf_map_returnsPositionInCluster() = runTest {
        val inside = GeoPoint(45.5, 10.5)
        geoStore.geotagged.value = listOf(GeotaggedImage(MediaId(1), inside, 2), GeotaggedImage(MediaId(2), inside, 1))

        val index = repository().indexOf(MediaSource.Map(GeoBounds(45.0, 10.0, 46.0, 11.0)), MediaId(2))

        assertEquals(1, index)
    }

    @Test
    fun imageDetails_locationNearTown_hasPlace() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1).copy(location = GeoPoint(45.5, 10.6))))

        assertEquals(desenzano, repository().imageDetails(MediaId(1))?.place)
    }

    @Test
    fun imageDetails_noLocation_hasNoPlace() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1)))

        assertNull(repository().imageDetails(MediaId(1))?.place)
    }

    @Test
    fun folderItemIds_returnsIdsOfThatFolder() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8)))

        assertEquals(listOf(MediaId(1)), repository().folderItemIds(7))
    }

    @Test
    fun virtualFolderItemIds_returnsIdsOfItsMembers() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 9)))

        assertEquals(listOf(MediaId(1)), repository().virtualFolderItemIds(listOf(7, 8)))
    }

    @Test
    fun imageDetails_unknownId_returnsNull() = runTest {
        assertNull(repository().imageDetails(MediaId(1)))
    }

    @Test
    fun imageDetails_includesExifFromFile() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1)))
        val exif = RawExif(null, null, null, iso = "400")

        assertEquals("400", repository(exif).imageDetails(MediaId(1))?.iso)
    }
}
