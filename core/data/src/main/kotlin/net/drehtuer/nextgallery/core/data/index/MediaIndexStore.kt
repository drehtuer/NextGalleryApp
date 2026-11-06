package net.drehtuer.nextgallery.core.data.index

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem

/** Port: the local image index. Implemented with Room in `:core:database`. */
interface MediaIndexStore {
    suspend fun upsertAll(items: List<MediaItem>)

    suspend fun deleteByIds(ids: Collection<Long>)

    suspend fun idsForVolume(volume: String): Set<Long>

    suspend fun getById(id: MediaId): MediaItem?

    /** Images of a folder, newest first. */
    fun pagedByBucket(bucketId: Long, config: PagingConfig): Flow<PagingData<MediaItem>>

    /** Ids of all images of a folder, in the order of [pagedByBucket]. */
    suspend fun idsInBucket(bucketId: Long): List<MediaId>

    /** Folders that contain indexed images, sorted by name; the cover is the newest image. */
    fun observeFolders(): Flow<List<Folder>>
}
