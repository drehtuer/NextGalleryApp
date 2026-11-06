package net.drehtuer.nextgallery.core.data.index

import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem

/** Port: the local image index. Implemented with Room in `:core:database`. */
interface MediaIndexStore {
    suspend fun upsertAll(items: List<MediaItem>)

    suspend fun deleteByIds(ids: Collection<Long>)

    suspend fun idsForVolume(volume: String): Set<Long>

    suspend fun getById(id: MediaId): MediaItem?

    fun observeCount(): Flow<Int>
}
