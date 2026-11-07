package net.drehtuer.nextgallery.core.data.media

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.VirtualFolder
import net.drehtuer.nextgallery.core.model.VirtualFolderSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DefaultFolderRepositoryTest {
    private val store = FakeMediaIndexStore()
    private val settings = MutableStateFlow(UserSettings())
    private val settingsRepository = mockk<SettingsRepository> { every { observeSettings() } returns settings }
    private val repository = DefaultFolderRepository(store, settingsRepository)
    private val virtualFolder = VirtualFolder("v1", "Everything", listOf(7, 8))

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
    fun observeVirtualFolders_addsUpTheMembers() = runTest {
        settings.value = UserSettings(virtualFolders = listOf(virtualFolder))
        store.folders.value = listOf(folder(7, itemCount = 2), folder(8, itemCount = 3), folder(9, itemCount = 5))

        repository.observeVirtualFolders().test {
            assertEquals(5, awaitItem().single().itemCount)
        }
    }

    @Test
    fun observeVirtualFolders_coverIsTheNewestImageOfTheMembers() = runTest {
        settings.value = UserSettings(virtualFolders = listOf(virtualFolder))
        store.folders.value = listOf(folder(7, coverDate = 10), folder(8, coverDate = 30), folder(9, coverDate = 50))

        repository.observeVirtualFolders().test {
            assertEquals(30L, awaitItem().single().coverItem?.dateTakenMillis)
        }
    }

    @Test
    fun observeVirtualFolders_memberNotIndexed_isIgnored() = runTest {
        settings.value = UserSettings(virtualFolders = listOf(virtualFolder))
        store.folders.value = listOf(folder(7, itemCount = 2))

        repository.observeVirtualFolders().test {
            assertEquals(2, awaitItem().single().itemCount)
        }
    }

    @Test
    fun observeVirtualFolders_noMemberIndexed_staysListedWithoutCover() = runTest {
        settings.value = UserSettings(virtualFolders = listOf(virtualFolder))

        repository.observeVirtualFolders().test {
            assertEquals(VirtualFolderSummary(virtualFolder, 0, null), awaitItem().single())
        }
    }

    @Test
    fun observeVirtualFolder_knownId_emitsIt() = runTest {
        settings.value = UserSettings(virtualFolders = listOf(virtualFolder))

        repository.observeVirtualFolder("v1").test {
            assertEquals(virtualFolder, awaitItem()?.folder)
        }
    }

    @Test
    fun observeVirtualFolder_unknownId_emitsNull() = runTest {
        repository.observeVirtualFolder("v1").test {
            assertNull(awaitItem())
        }
    }

    private fun folder(bucketId: Long, itemCount: Int = 1, coverDate: Long = 1) = Folder(
        bucketId = bucketId,
        name = "Camera",
        relativePath = "DCIM/Camera/",
        itemCount = itemCount,
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
            dateTakenMillis = coverDate,
            dateSource = DateSource.DATE_TAKEN,
            takenLocalDate = LocalDate.of(2024, 3, 15),
            dateModifiedSeconds = 1,
            generationModified = 1,
            location = null,
        ),
    )
}
