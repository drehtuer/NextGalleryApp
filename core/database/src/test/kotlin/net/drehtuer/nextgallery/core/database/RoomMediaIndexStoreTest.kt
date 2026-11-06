package net.drehtuer.nextgallery.core.database

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomMediaIndexStoreTest {
    private val dao = mockk<MediaDao>(relaxed = true)
    private val store = RoomMediaIndexStore(dao)

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
}
