package net.drehtuer.nextgallery.core.data.nextcloud

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpException
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRemoteShareRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val webDav = mockk<WebDavClient>(relaxed = true)
    private val store = FakeAccountStore()
    private val online = MutableStateFlow(true)
    private val connectivity = object : ConnectivitySource {
        override val isOnline = online
    }

    private inner class TempShareCache : RemoteShareCache {
        var clears = 0
        val files = mutableListOf<File>()

        override fun clear() {
            clears++
            files.forEach { it.delete() }
            files.clear()
        }

        override fun newFile(index: Int, name: String): File = File(folder.root, "$index-$name").also { files += it }

        override fun contentUri(file: File) = "content://share/${file.name}"
    }

    private val cache = TempShareCache()
    private val repository = DefaultRemoteShareRepository(
        testRunner(store),
        webDav,
        cache,
        connectivity,
        UnconfinedTestDispatcher(),
    )

    private val a = RemoteItem("/P/a.jpg", "a.jpg", false, mime = "image/jpeg")
    private val b = RemoteItem("/P/b.png", "b.png", false, mime = "image/png")

    @Test
    fun prepare_reportsProgressPerImageThenReady() = runTest {
        val states = repository.prepare(listOf(a, b)).toList()

        assertEquals(
            listOf(
                RemoteShareProgress.Downloading(1, 2),
                RemoteShareProgress.Downloading(2, 2),
                RemoteShareProgress.Ready(
                    ShareSpec(listOf("content://share/0-a.jpg", "content://share/1-b.png"), "image/*"),
                ),
            ),
            states,
        )
    }

    @Test
    fun prepare_writesTheDownloadToTheTemporaryFile() = runTest {
        coEvery { webDav.download(TEST_ACCOUNT, "/P/a.jpg", any(), any()) } coAnswers {
            arg<OutputStream>(2).write("image".toByteArray())
        }

        repository.prepare(listOf(a)).toList()

        assertEquals("image", File(folder.root, "0-a.jpg").readText())
    }

    @Test
    fun prepare_sameTypes_keepsTheType() = runTest {
        val ready = repository.prepare(listOf(a)).toList().last() as RemoteShareProgress.Ready

        assertEquals("image/jpeg", ready.spec.mimeType)
    }

    @Test
    fun prepare_removesTheFilesOfTheShareBefore() = runTest {
        repository.prepare(listOf(a)).toList()
        val first = File(folder.root, "0-a.jpg")
        assertTrue(first.exists())

        repository.prepare(listOf(b)).toList()

        assertFalse(first.exists())
    }

    @Test
    fun prepare_downloadFails_reportsTheErrorAndCleansUp() = runTest {
        coEvery { webDav.download(any(), "/P/b.png", any(), any()) } throws NextcloudHttpException(404)

        val states = repository.prepare(listOf(a, b)).toList()

        assertEquals(RemoteShareProgress.Failed(AppError.NotFound), states.last())
        assertTrue(cache.files.isEmpty())
    }

    @Test
    fun prepare_offline_failsWithoutDownloading() = runTest {
        online.value = false

        val states = repository.prepare(listOf(a)).toList()

        assertEquals(listOf<RemoteShareProgress>(RemoteShareProgress.Failed(AppError.Network(null))), states)
        coVerify(exactly = 0) { webDav.download(any(), any(), any(), any()) }
    }

    @Test
    fun prepare_loggedOut_failsWithoutNetwork() = runTest {
        store.account.value = null

        val states = repository.prepare(listOf(a)).toList()

        assertEquals(RemoteShareProgress.Failed(AppError.NotLoggedIn), states.last())
        coVerify(exactly = 0) { webDav.download(any(), any(), any(), any()) }
    }

    @Test
    fun prepare_loggedOutDuringADownload_failsAndCleansUp() = runTest {
        coEvery { webDav.download(TEST_ACCOUNT, "/P/a.jpg", any(), any()) } coAnswers {
            arg<OutputStream>(2).write("image".toByteArray())
            store.account.value = null
        }

        val states = repository.prepare(listOf(a)).toList()

        assertEquals(RemoteShareProgress.Failed(AppError.NotLoggedIn), states.last())
        assertFalse(File(folder.root, "0-a.jpg").exists())
    }

    @Test
    fun prepare_anotherAccountDuringADownload_stopsAndCleansUp() = runTest {
        coEvery { webDav.download(TEST_ACCOUNT, "/P/a.jpg", any(), any()) } coAnswers {
            store.account.value = TEST_ACCOUNT.copy(userId = "someone-else")
        }

        val states = repository.prepare(listOf(a, b)).toList()

        assertEquals(RemoteShareProgress.Failed(AppError.NotLoggedIn), states.last())
        assertTrue(cache.files.isEmpty())
        coVerify(exactly = 0) { webDav.download(any(), "/P/b.png", any(), any()) }
    }

    @Test
    fun clear_removesTheTemporaryFiles() = runTest {
        repository.prepare(listOf(a)).toList()
        val clearsBefore = cache.clears

        repository.clear()

        assertEquals(clearsBefore + 1, cache.clears)
        assertFalse(File(folder.root, "0-a.jpg").exists())
    }

    @Test
    fun safeFileName_plainName_staysTheSame() {
        assertEquals("IMG 1.jpg", safeFileName("IMG 1.jpg"))
    }

    @Test
    fun safeFileName_pathInName_keepsTheLastPart() {
        assertEquals("passwd", safeFileName("../../etc/passwd"))
    }

    @Test
    fun safeFileName_backslashes_keepTheLastPart() {
        assertEquals("evil.jpg", safeFileName("..\\..\\evil.jpg"))
    }

    @Test
    fun safeFileName_dotsOnly_getsAPlainName() {
        assertEquals("image", safeFileName(".."))
    }

    @Test
    fun safeFileName_controlCharacters_areRemoved() {
        assertEquals("ab.jpg", safeFileName("a\u0000b.jpg"))
    }

    @Test
    fun safeFileName_controlCharacterBetweenDots_getsAPlainName() {
        assertEquals("image", safeFileName(".\u0000."))
    }

    @Test
    fun safeFileName_controlCharacterHidingADotName_getsAPlainName() {
        assertEquals("image", safeFileName("a/\u0007..\u0007"))
    }

    @Test
    fun safeFileName_spacesAroundDots_getsAPlainName() {
        assertEquals("image", safeFileName(" . . "))
    }

    @Test
    fun safeFileName_controlCharacterHidingASpace_isTrimmedAfterwards() {
        assertEquals("a.jpg", safeFileName(" \u0000 a.jpg \u0000 "))
    }
}
