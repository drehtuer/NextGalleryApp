package net.drehtuer.nextgallery.core.data.media

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import javax.inject.Inject

interface MediaRepository {
    suspend fun getItem(id: MediaId): MediaItem?

    /** Images of a folder, newest first, with placeholders so that a grid knows the total count. */
    fun pagedFolderItems(bucketId: Long): Flow<PagingData<MediaItem>>

    suspend fun folderItemIds(bucketId: Long): List<MediaId>
}

class DefaultMediaRepository @Inject constructor(private val indexStore: MediaIndexStore) : MediaRepository {
    override suspend fun getItem(id: MediaId): MediaItem? = indexStore.getById(id)

    override fun pagedFolderItems(bucketId: Long): Flow<PagingData<MediaItem>> =
        indexStore.pagedByBucket(bucketId, GRID_PAGING_CONFIG)

    override suspend fun folderItemIds(bucketId: Long): List<MediaId> = indexStore.idsInBucket(bucketId)

    internal companion object {
        private const val PAGE_SIZE = 120

        /**
         * A jump beyond the threshold (fast scrollbar) loads the target page directly, instead of every page
         * in between.
         */
        val GRID_PAGING_CONFIG = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = 2 * PAGE_SIZE,
            enablePlaceholders = true,
            jumpThreshold = 3 * PAGE_SIZE,
        )
    }
}
