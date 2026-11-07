package net.drehtuer.nextgallery.core.data.media

import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeExifSource
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.FakeTimelineStore
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.RawExif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMediaRepositoryTest {
    private val store = FakeMediaIndexStore()
    private val timelineStore = FakeTimelineStore()
    private val folder = MediaSource.Folder(7)

    private fun repository(exif: RawExif? = null) = DefaultMediaRepository(store, timelineStore, FakeExifSource(exif))

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
    fun pagedItems_timeline_returnsItemsOfAllFolders() = runTest {
        timelineStore.items = listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8))

        val snapshot = repository().pagedItems(MediaSource.Timeline).asSnapshot()

        assertEquals(listOf(MediaId(1), MediaId(2)), snapshot.map { it.id })
    }

    @Test
    fun pagedItems_usesPlaceholders() {
        repository().pagedItems(folder)

        assertTrue(store.pagingConfig?.enablePlaceholders == true)
    }

    @Test
    fun pagedItems_jumpsToFarAwayPages() {
        repository().pagedItems(folder)

        assertEquals(360, store.pagingConfig?.jumpThreshold)
    }

    @Test
    fun indexOf_folder_returnsPositionInFolder() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1), mediaItem(id = 2)))

        assertEquals(1, repository().indexOf(folder, MediaId(2)))
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
    fun folderItemIds_returnsIdsOfThatFolder() = runTest {
        store.upsertAll(listOf(mediaItem(id = 1, bucketId = 7), mediaItem(id = 2, bucketId = 8)))

        assertEquals(listOf(MediaId(1)), repository().folderItemIds(7))
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
