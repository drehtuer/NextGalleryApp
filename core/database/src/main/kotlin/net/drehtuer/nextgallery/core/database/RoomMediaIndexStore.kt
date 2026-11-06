package net.drehtuer.nextgallery.core.database

import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import javax.inject.Inject

class RoomMediaIndexStore @Inject constructor(private val dao: MediaDao) : MediaIndexStore {
    override suspend fun upsertAll(items: List<MediaItem>) = dao.upsertAll(items.map { it.toEntity() })

    override suspend fun deleteByIds(ids: Collection<Long>) {
        ids.chunked(SQLITE_MAX_VARIABLES).forEach { dao.deleteByIds(it) }
    }

    override suspend fun idsForVolume(volume: String): Set<Long> = dao.idsForVolume(volume).toSet()

    override suspend fun getById(id: MediaId): MediaItem? = dao.getById(id.value)?.toModel()

    override fun observeCount(): Flow<Int> = dao.observeCount()

    internal companion object {
        /** Stays below SQLite's limit of bound variables per statement. */
        const val SQLITE_MAX_VARIABLES = 900
    }
}
