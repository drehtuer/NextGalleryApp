package net.drehtuer.nextgallery.core.database

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomMediaIndexStoreTest {
    private val dao = mockk<MediaDao>(relaxed = true)
    private val folderDao = mockk<FolderDao>()
    private val store = RoomMediaIndexStore(dao, folderDao)

    @Test
    fun deleteByIds_manyIds_deletesInChunksBelowSqliteLimit() = runTest {
        store.deleteByIds((1L..1_801L).toList())

        coVerify(exactly = 3) { dao.deleteByIds(any()) }
    }

    @Test
    fun upsertAll_passesMappedEntitiesToDao() = runTest {
        store.upsertAll(listOf(mediaEntity(1).toModel()))

        coVerify { dao.upsertAll(listOf(mediaEntity(1))) }
    }

    @Test
    fun getById_mapsEntityToModel() = runTest {
        coEvery { dao.getById(1) } returns mediaEntity(1)

        assertEquals(mediaEntity(1).toModel(), store.getById(mediaEntity(1).toModel().id))
    }

    @Test
    fun idsForVolume_returnsDaoIdsAsSet() = runTest {
        coEvery { dao.idsForVolume("v") } returns listOf(1L, 2L)

        assertEquals(setOf(1L, 2L), store.idsForVolume("v"))
    }

    @Test
    fun getByIds_manyIds_readsInChunksBelowSqliteLimit() = runTest {
        coEvery { dao.getByIds(any()) } returns emptyList()

        store.getByIds((1L..1_801L).map { MediaId(it) })

        coVerify(exactly = 3) { dao.getByIds(any()) }
    }

    @Test
    fun indexInBucket_passesIdToDao() = runTest {
        coEvery { dao.indexInBucket(7, 1) } returns 4

        assertEquals(4, store.indexInBucket(7, MediaId(1)))
    }

    @Test
    fun idsInBucket_mapsDaoIdsToMediaIds() = runTest {
        coEvery { dao.idsInBucket(7) } returns listOf(1L)

        assertEquals(listOf(MediaId(1)), store.idsInBucket(7))
    }

    @Test
    fun observeFolders_mapsRowsToFolders() = runTest {
        every { folderDao.observeFolders() } returns flowOf(listOf(FolderRow(mediaEntity(1), itemCount = 3)))

        assertEquals(3, store.observeFolders().first().single().itemCount)
    }
}
