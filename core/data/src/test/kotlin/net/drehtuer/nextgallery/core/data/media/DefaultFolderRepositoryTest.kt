package net.drehtuer.nextgallery.core.data.media

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaChangeSource
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaScanSource
import net.drehtuer.nextgallery.core.data.fakes.scannedImage
import net.drehtuer.nextgallery.core.data.index.IndexScheduler
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.DeviceFolder
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFolderRepositoryTest {
    private val store = FakeMediaIndexStore()
    private val scanSource = FakeMediaScanSource()
    private val changeSource = FakeMediaChangeSource()
    private val repository = DefaultFolderRepository(store, scanSource, changeSource)

    @Test
    fun observeFolders_emitsChangesFromStore() = runTest {
        repository.observeFolders().test {
            assertEquals(emptyList<Folder>(), awaitItem())

            store.folders.value = listOf(folder(bucketId = 7))

            assertEquals(listOf(folder(bucketId = 7)), awaitItem())
        }
    }

    @Test
    fun observeFolder_knownBucket_emitsFolder() = runTest {
        store.folders.value = listOf(folder(bucketId = 7), folder(bucketId = 8))

        repository.observeFolder(8).test {
            assertEquals(folder(bucketId = 8), awaitItem())
        }
    }

    @Test
    fun observeFolder_unknownBucket_emitsNull() = runTest {
        repository.observeFolder(8).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun observeAllDeviceFolders_countsImagesPerFolderSortedByPath() = runTest {
        scanSource.images = listOf(
            scannedImage(1, "Pictures/Holiday/"),
            scannedImage(2, "DCIM/Camera/"),
            scannedImage(3, "DCIM/Camera/"),
        )

        repository.observeAllDeviceFolders().test {
            assertEquals(
                listOf(DeviceFolder("Camera", "DCIM/Camera", 2), DeviceFolder("Holiday", "Pictures/Holiday", 1)),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllDeviceFolders_sameFolderOnTwoVolumes_isOneFolder() = runTest {
        scanSource.images = listOf(
            scannedImage(1, "DCIM/Camera/", volumeName = "external_primary"),
            scannedImage(2, "dcim/camera/", volumeName = "sdcard"),
        )

        repository.observeAllDeviceFolders().test {
            assertEquals(2, awaitItem().single().itemCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllDeviceFolders_storageRoot_isLeftOut() = runTest {
        scanSource.images = listOf(scannedImage(1, "/"))

        repository.observeAllDeviceFolders().test {
            assertEquals(emptyList<DeviceFolder>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllDeviceFolders_mediaStoreChange_scansAgain() = runTest {
        repository.observeAllDeviceFolders().test {
            assertEquals(emptyList<DeviceFolder>(), awaitItem())
            scanSource.images = listOf(scannedImage(1, "DCIM/Camera/"))

            changeSource.events.emit(Unit)
            advanceTimeBy(IndexScheduler.CHANGE_DEBOUNCE_MILLIS + 1)

            assertEquals(listOf(DeviceFolder("Camera", "DCIM/Camera", 1)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun folder(bucketId: Long) = Folder(
        bucketId = bucketId,
        name = "Camera",
        relativePath = "DCIM/Camera/",
        itemCount = 1,
        coverItem = MediaItem(
            id = MediaId(1),
            volumeName = "external_primary",
            contentUri = "content://media/1",
            bucketId = bucketId,
            bucketName = "Camera",
            relativePath = "DCIM/Camera/",
            displayName = "IMG_1.jpg",
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
        ),
    )
}
