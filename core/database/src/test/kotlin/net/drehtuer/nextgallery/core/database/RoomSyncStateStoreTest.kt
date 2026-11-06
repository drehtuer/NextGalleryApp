package net.drehtuer.nextgallery.core.database

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.index.SyncState
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomSyncStateStoreTest {
    private val dao = mockk<SyncStateDao>(relaxed = true)
    private val store = RoomSyncStateStore(dao)
    private val state = SyncState("external_primary", 12, "v1", 5_000)

    @Test
    fun get_mapsEntityToModel() = runTest {
        coEvery { dao.get("external_primary") } returns state.toEntity()

        assertEquals(state, store.get("external_primary"))
    }

    @Test
    fun put_passesMappedEntityToDao() = runTest {
        store.put(state)

        coVerify { dao.put(state.toEntity()) }
    }
}
