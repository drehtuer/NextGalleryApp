package net.drehtuer.nextgallery.core.database

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.model.RemoteItem
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomRemoteCacheStoreTest {
    private val dao = mockk<RemoteCacheDao>(relaxed = true)
    private val clock = mockk<Clock> { every { nowMillis() } returns 500 }
    private val store = RoomRemoteCacheStore(dao, clock)

    private val item = RemoteItem(
        path = "/A/a.jpg",
        name = "a.jpg",
        isDirectory = false,
        size = 10,
        mime = "image/jpeg",
        etag = "e",
        lastModifiedMillis = 3,
        fileId = 42,
        checksums = "SHA1:aa",
        permissions = "RGD",
        hasPreview = false,
        childCount = null,
    )

    @Test
    fun replaceFolder_mapsItemsAndAddsTheTime() = runTest {
        store.replaceFolder("/A", listOf(item))

        coVerify { dao.replaceFolder("/A", listOf(item.toEntity(500))) }
    }

    @Test
    fun entityMapping_keepsAllFields() {
        assertEquals(item, item.toEntity(500).toModel())
    }

    @Test
    fun entityMapping_usesTheParentPathOfTheItem() {
        assertEquals("/A", item.toEntity(500).parentPath)
    }

    @Test
    fun get_mapsEntities() = runTest {
        coEvery { dao.getFolder("/A") } returns listOf(item.toEntity(500))

        assertEquals(listOf(item), store.get("/A"))
    }

    @Test
    fun get_sortsFoldersByNameThenImagesNewestFirst() = runTest {
        val folderB = RemoteItem("/A/B", "B", true)
        val folderA = RemoteItem("/A/a", "a", true)
        val older = RemoteItem("/A/o.jpg", "o.jpg", false, lastModifiedMillis = 1)
        val newer = RemoteItem("/A/n.jpg", "n.jpg", false, lastModifiedMillis = 2)
        coEvery { dao.getFolder("/A") } returns listOf(older, folderB, newer, folderA).map { it.toEntity(1) }

        assertEquals(listOf(folderA, folderB, newer, older), store.get("/A"))
    }

    @Test
    fun clear_clearsTheTable() = runTest {
        store.clear()

        coVerify { dao.clear() }
    }
}
