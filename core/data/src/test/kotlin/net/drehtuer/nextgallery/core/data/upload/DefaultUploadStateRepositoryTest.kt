package net.drehtuer.nextgallery.core.data.upload

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.fakes.FakeClock
import net.drehtuer.nextgallery.core.data.fakes.TestDispatcherProvider
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.data.nextcloud.FakeAccountStore
import net.drehtuer.nextgallery.core.data.nextcloud.TEST_ACCOUNT
import net.drehtuer.nextgallery.core.data.nextcloud.testRunner
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.UploadState
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpException
import net.drehtuer.nextgallery.core.nextcloud.RemoteFolder
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUploadStateRepositoryTest {
    private val camera = mediaItem(1).copy(relativePath = "DCIM/Camera/", displayName = "a.jpg", sizeBytes = 100)
    private val screenshots = mediaItem(
        2,
    ).copy(relativePath = "Pictures/Shots/", displayName = "b.png", sizeBytes = 200)
    private val accounts = FakeAccountStore()
    private val webDav = mockk<WebDavClient>()
    private val store = FakeUploadStateStore(listOf(camera))
    private val settings = mockk<SettingsRepository> {
        every { observeSettings() } returns flowOf(UserSettings(remoteRoot = "/Photos"))
    }
    private var readsOfImages = 0
    private val content = MediaContentSource {
        readsOfImages++
        "abc".byteInputStream()
    }

    private fun repository() = DefaultUploadStateRepository(
        accounts = accounts,
        runner = testRunner(accounts),
        webDav = webDav,
        settings = settings,
        store = store,
        checksums = LocalChecksums(content, TestDispatcherProvider(UnconfinedTestDispatcher())),
        clock = FakeClock(now = 500),
    )

    private fun serverFile(name: String = "a.jpg", size: Long = 100, checksums: String? = null) = RemoteItem(
        "/Photos/DCIM/Camera/$name",
        name,
        isDirectory = false,
        size = size,
        etag = "f",
        checksums = checksums,
    )

    private fun serverHas(vararg files: RemoteItem, folderEtag: String? = "folder-1") {
        val self = folderEtag?.let { RemoteItem("/Photos/DCIM/Camera", "Camera", isDirectory = true, etag = it) }
        coEvery { webDav.list(TEST_ACCOUNT, "/Photos/DCIM/Camera") } returns RemoteFolder(self, files.toList())
    }

    private fun stateOfCamera() = store.records.value[camera.id]?.state

    @Test
    fun refreshAll_listsTheFolderUnderTheRemoteRoot() = runTest {
        serverHas(serverFile())

        repository().refreshAll()

        coVerify(exactly = 1) { webDav.list(TEST_ACCOUNT, "/Photos/DCIM/Camera") }
    }

    @Test
    fun refreshAll_storesWhatItFound() = runTest {
        serverHas(serverFile())

        val result = repository().refreshAll()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(UploadState.Uploaded(verifiedByChecksum = false), stateOfCamera())
    }

    @Test
    fun refreshAll_timesTheRecord() = runTest {
        serverHas(serverFile())

        repository().refreshAll()

        assertEquals(500L, store.records.value[camera.id]?.checkedAtMillis)
    }

    @Test
    fun refreshAll_serverChecksum_confirmsAgainstTheImage() = runTest {
        serverHas(serverFile(checksums = "MD5:900150983cd24fb0d6963f7d28e17f72"))

        repository().refreshAll()

        assertEquals(UploadState.Uploaded(verifiedByChecksum = true), stateOfCamera())
    }

    @Test
    fun refreshAll_loggedOut_doesNothingAndSucceeds() = runTest {
        accounts.account.value = null

        val result = repository().refreshAll()

        assertEquals(AppResult.Success(Unit), result)
        coVerify(exactly = 0) { webDav.list(any(), any()) }
        assertEquals(0, store.upserts)
    }

    @Test
    fun refreshAll_folderNotOnTheServer_marksItsImagesNotUploaded() = runTest {
        coEvery { webDav.list(any(), any()) } throws NextcloudHttpException(404)

        val result = repository().refreshAll()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(UploadState.NotUploaded, stateOfCamera())
    }

    @Test
    fun refreshAll_offline_failsSoThatItCanBeRetried() = runTest {
        coEvery { webDav.list(any(), any()) } throws IOException("offline")

        val result = repository().refreshAll()

        assertEquals(AppResult.Failure(AppError.Network("offline")), result)
    }

    @Test
    fun refreshAll_offline_keepsThePreviousRecords() = runTest {
        coEvery { webDav.list(any(), any()) } throws IOException("offline")

        repository().refreshAll()

        assertEquals(0, store.upserts)
    }

    @Test
    fun refreshAll_passwordRejected_fails() = runTest {
        coEvery { webDav.list(any(), any()) } throws NextcloudHttpException(401)

        assertEquals(AppResult.Failure(AppError.Auth), repository().refreshAll())
    }

    @Test
    fun refreshAll_noPermissionForOneFolder_goesOnWithTheNext() = runTest {
        store.items = listOf(camera, screenshots)
        coEvery { webDav.list(any(), "/Photos/DCIM/Camera") } throws NextcloudHttpException(403)
        coEvery { webDav.list(any(), "/Photos/Pictures/Shots") } returns RemoteFolder(null, emptyList())

        val result = repository().refreshAll()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(setOf(screenshots.id), store.records.value.keys)
    }

    @Test
    fun refreshAll_loggedOutWhileListing_doesNotStoreTheResult() = runTest {
        coEvery { webDav.list(any(), any()) } coAnswers {
            accounts.account.value = null
            RemoteFolder(null, emptyList())
        }

        val result = repository().refreshAll()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(0, store.upserts)
    }

    @Test
    fun refreshAll_otherAccountWhileListing_doesNotStoreTheResult() = runTest {
        coEvery { webDav.list(any(), any()) } coAnswers {
            accounts.account.value = TEST_ACCOUNT.copy(userId = "other")
            RemoteFolder(null, emptyList())
        }

        repository().refreshAll()

        assertEquals(0, store.upserts)
    }

    @Test
    fun refreshAll_storeFails_isAFailure() = runTest {
        val failing = mockk<UploadStateStore> { coEvery { localFolders() } throws IOException("disk") }
        val repository = DefaultUploadStateRepository(
            accounts,
            testRunner(accounts),
            webDav,
            settings,
            failing,
            LocalChecksums(content, TestDispatcherProvider(UnconfinedTestDispatcher())),
            FakeClock(),
        )

        assertEquals(AppResult.Failure(AppError.Io("disk")), repository.refreshAll())
    }

    @Test
    fun refreshAll_nothingChanged_skipsTheComparison() = runTest {
        serverHas(serverFile(), folderEtag = "folder-1")
        val repository = repository()
        repository.refreshAll()

        repository.refreshAll()

        assertEquals(1, store.upserts)
    }

    @Test
    fun refreshAll_serverFolderChanged_comparesAgain() = runTest {
        serverHas(serverFile(), folderEtag = "folder-1")
        val repository = repository()
        repository.refreshAll()
        serverHas(serverFile(), folderEtag = "folder-2")

        repository.refreshAll()

        assertEquals(2, store.upserts)
    }

    @Test
    fun refreshAll_imageChanged_comparesAgain() = runTest {
        serverHas(serverFile(), folderEtag = "folder-1")
        val repository = repository()
        repository.refreshAll()
        store.items = listOf(camera.copy(dateModifiedSeconds = 99))

        repository.refreshAll()

        assertEquals(2, store.upserts)
    }

    @Test
    fun refreshAll_newImage_comparesAgain() = runTest {
        serverHas(serverFile(), folderEtag = "folder-1")
        val repository = repository()
        repository.refreshAll()
        store.items = listOf(camera, camera.copy(id = MediaId(3), displayName = "c.jpg"))

        repository.refreshAll()

        assertEquals(2, store.upserts)
    }

    @Test
    fun refreshAll_serverWithoutFolderEtag_comparesAgain() = runTest {
        serverHas(serverFile(), folderEtag = null)
        val repository = repository()
        repository.refreshAll()

        repository.refreshAll()

        assertEquals(2, store.upserts)
    }

    @Test
    fun refreshAll_nameAndSizeDoNotMatch_readsNoImage() = runTest {
        serverHas(serverFile(size = 1, checksums = "SHA1:aa"))

        repository().refreshAll()

        assertEquals(0, readsOfImages)
    }

    @Test
    fun refreshAll_alreadyConfirmed_readsTheImageOnlyOnce() = runTest {
        serverHas(serverFile(checksums = "MD5:900150983cd24fb0d6963f7d28e17f72"), folderEtag = "folder-1")
        val repository = repository()
        repository.refreshAll()
        serverHas(serverFile(checksums = "MD5:900150983cd24fb0d6963f7d28e17f72"), folderEtag = "folder-2")

        repository.refreshAll()

        assertEquals(1, readsOfImages)
    }

    @Test
    fun observeBadges_loggedOut_isEmpty() = runTest {
        accounts.account.value = null
        store.states.value = mapOf(camera.id to UploadState.NotUploaded)

        repository().observeBadges(flowOf(setOf(camera.id))).test {
            assertEquals(emptyMap<MediaId, UploadState>(), awaitItem())
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun observeBadges_startsEmpty_beforeAnythingIsRead() = runTest {
        repository().observeBadges(flowOf(setOf(camera.id))).test {
            assertEquals(emptyMap<MediaId, UploadState>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeBadges_knownState_isReported() = runTest {
        store.states.value = mapOf(camera.id to UploadState.NotUploaded)

        repository().observeBadges(flowOf(setOf(camera.id))).test {
            skipItems(1)
            assertEquals(mapOf(camera.id to UploadState.NotUploaded), awaitItem())
            cancel()
        }
    }

    @Test
    fun observeBadges_noRecord_isUnknown() = runTest {
        repository().observeBadges(flowOf(setOf(camera.id))).test {
            skipItems(1)
            assertEquals(mapOf(camera.id to UploadState.Unknown), awaitItem())
            cancel()
        }
    }

    @Test
    fun observeBadges_otherImagesBecomeVisible_followsThem() = runTest {
        val visible = MutableStateFlow(setOf(camera.id))

        repository().observeBadges(visible).test {
            skipItems(2)
            visible.value = setOf(screenshots.id)
            assertEquals(setOf(screenshots.id), awaitItem().keys)
            cancel()
        }
    }

    @Test
    fun observeBadges_logout_clearsTheBadges() = runTest {
        repository().observeBadges(flowOf(setOf(camera.id))).test {
            skipItems(2)
            accounts.account.value = null
            assertTrue(awaitItem().isEmpty())
            cancel()
        }
    }
}
