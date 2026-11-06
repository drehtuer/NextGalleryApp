package net.drehtuer.nextgallery.core.data.media

import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DefaultMediaRepositoryTest {
    private val store = FakeMediaIndexStore()

    @Test
    fun getItem_unknownId_returnsNull() = runTest {
        assertNull(DefaultMediaRepository(store).getItem(MediaId(1)))
    }

    @Test
    fun pagedFolderItems_returnsItemsOfThatFolder() = runTest {
        store.upsertAll(listOf(item(id = 1, bucketId = 7), item(id = 2, bucketId = 8)))

        val snapshot = DefaultMediaRepository(store).pagedFolderItems(7).asSnapshot()

        assertEquals(listOf(MediaId(1)), snapshot.map { it.id })
    }

    @Test
    fun pagedFolderItems_usesPlaceholders() {
        DefaultMediaRepository(store).pagedFolderItems(7)

        assertTrue(store.pagingConfig?.enablePlaceholders == true)
    }

    @Test
    fun pagedFolderItems_jumpsToFarAwayPages() {
        DefaultMediaRepository(store).pagedFolderItems(7)

        assertEquals(360, store.pagingConfig?.jumpThreshold)
    }

    @Test
    fun folderItemIds_returnsIdsOfThatFolder() = runTest {
        store.upsertAll(listOf(item(id = 1, bucketId = 7), item(id = 2, bucketId = 8)))

        assertEquals(listOf(MediaId(1)), DefaultMediaRepository(store).folderItemIds(7))
    }

    private fun item(id: Long, bucketId: Long) = MediaItem(
        id = MediaId(id),
        volumeName = "external_primary",
        contentUri = "content://media/$id",
        bucketId = bucketId,
        bucketName = "Camera",
        relativePath = "DCIM/Camera/",
        displayName = "IMG_$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 1,
        width = 1,
        height = 1,
        orientation = 0,
        dateTakenMillis = 1,
        dateSource = DateSource.DATE_TAKEN,
        takenLocalDate = LocalDate.of(2024, 3, 15),
        dateModifiedSeconds = 1,
        generationModified = 1,
        location = null,
    )
}
