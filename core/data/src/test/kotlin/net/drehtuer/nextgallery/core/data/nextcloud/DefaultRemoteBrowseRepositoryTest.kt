package net.drehtuer.nextgallery.core.data.nextcloud

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.nextcloud.RemoteFolder
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultRemoteBrowseRepositoryTest {
    private val webDav = mockk<WebDavClient>()
    private val cache = mockk<RemoteCacheStore>(relaxed = true)
    private val store = FakeAccountStore()
    private val repository = DefaultRemoteBrowseRepository(store, testRunner(store), webDav, cache)

    private val image = RemoteItem("/P/a.jpg", "a.jpg", false, mime = "image/jpeg", lastModifiedMillis = 1)
    private val newerImage = RemoteItem("/P/b.png", "b.png", false, mime = "image/png", lastModifiedMillis = 2)
    private val pdf = RemoteItem("/P/c.pdf", "c.pdf", false, mime = "application/pdf")
    private val noMime = RemoteItem("/P/d", "d", false)
    private val folderB = RemoteItem("/P/B", "B", true)
    private val folderA = RemoteItem("/P/a", "a", true)

    private fun serverReturns(vararg items: RemoteItem) {
        coEvery { webDav.list(TEST_ACCOUNT, "/P") } returns RemoteFolder(null, items.toList())
    }

    @Test
    fun list_loggedOut_emitsNotLoggedInWithoutNetworkOrCache() = runTest {
        store.account.value = null

        val listings = repository.list("/P").toList()

        assertEquals(listOf(RemoteListing("/P", emptyList(), error = AppError.NotLoggedIn)), listings)
        coVerify(exactly = 0) { webDav.list(any(), any()) }
        coVerify(exactly = 0) { cache.get(any()) }
    }

    @Test
    fun list_emitsCachedFirstWhileRefreshing() = runTest {
        coEvery { cache.get("/P") } returns listOf(image)
        serverReturns(image)

        val first = repository.list("/P").toList().first()

        assertEquals(RemoteListing("/P", listOf(image), isRefreshing = true), first)
    }

    @Test
    fun list_success_emitsWhatTheServerHas() = runTest {
        coEvery { cache.get("/P") } returns emptyList()
        serverReturns(image)

        assertEquals(RemoteListing("/P", listOf(image)), repository.list("/P").toList().last())
    }

    @Test
    fun list_keepsFoldersAndSupportedImagesOnly() = runTest {
        coEvery { cache.get("/P") } returns emptyList()
        serverReturns(pdf, noMime, image, folderA)

        assertEquals(listOf(folderA, image), repository.list("/P").toList().last().items)
    }

    @Test
    fun list_sortsFoldersByNameThenImagesNewestFirst() = runTest {
        coEvery { cache.get("/P") } returns emptyList()
        serverReturns(image, folderB, newerImage, folderA)

        assertEquals(listOf(folderA, folderB, newerImage, image), repository.list("/P").toList().last().items)
    }

    @Test
    fun list_success_replacesTheCache() = runTest {
        coEvery { cache.get("/P") } returns emptyList()
        serverReturns(image, pdf)

        repository.list("/P").toList()

        coVerify { cache.replaceFolder("/P", listOf(image)) }
    }

    @Test
    fun list_offline_keepsCachedItemsWithTheError() = runTest {
        coEvery { cache.get("/P") } returns listOf(image)
        coEvery { webDav.list(any(), any()) } throws IOException("offline")

        val last = repository.list("/P").toList().last()

        assertEquals(RemoteListing("/P", listOf(image), error = AppError.Network("offline")), last)
    }

    @Test
    fun list_failure_leavesTheCacheAlone() = runTest {
        coEvery { cache.get("/P") } returns listOf(image)
        coEvery { webDav.list(any(), any()) } throws IOException("offline")

        repository.list("/P").toList()

        coVerify(exactly = 0) { cache.replaceFolder(any(), any()) }
    }

    @Test
    fun list_normalizesThePath() = runTest {
        coEvery { cache.get("/P") } returns emptyList()
        serverReturns()

        assertTrue(repository.list("P/").toList().all { it.path == "/P" })
    }
}
