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

    suspend fun getByIds(ids: Collection<MediaId>): List<MediaItem>

    /** Images of one folder or of several together, newest first. */
    fun pagedByBuckets(bucketIds: List<Long>, config: PagingConfig): Flow<PagingData<MediaItem>>

    /** Ids of all images of those folders, in the order of [pagedByBuckets]. */
    suspend fun idsInBuckets(bucketIds: List<Long>): List<MediaId>

    /** Position of an image in [pagedByBuckets]. */
    suspend fun indexInBuckets(bucketIds: List<Long>, id: MediaId): Int

    /** Folders that contain indexed images, sorted by name; the cover is the newest image. */
    fun observeFolders(): Flow<List<Folder>>
}
