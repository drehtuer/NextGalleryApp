package net.drehtuer.nextgallery.core.database

import net.drehtuer.nextgallery.core.data.index.SyncState
import net.drehtuer.nextgallery.core.data.index.SyncStateStore
import javax.inject.Inject

class RoomSyncStateStore @Inject constructor(private val dao: SyncStateDao) : SyncStateStore {
    override suspend fun get(volume: String): SyncState? = dao.get(volume)?.toModel()

    override suspend fun put(state: SyncState) = dao.put(state.toEntity())

    override suspend fun hasAny(): Boolean = dao.hasAny()
}
