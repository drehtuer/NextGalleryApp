package net.drehtuer.nextgallery.core.data.media

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.data.index.ExifSource
import net.drehtuer.nextgallery.core.data.index.GeoIndexStore
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.index.TimelineStore
import net.drehtuer.nextgallery.core.data.map.PlaceNamer
import net.drehtuer.nextgallery.core.model.ImageDetails
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.MediaSource
import javax.inject.Inject

interface MediaRepository {
    suspend fun getItem(id: MediaId): MediaItem?

    /** Images of a source, newest first, with placeholders so that a grid or pager knows the total count. */
    fun pagedItems(source: MediaSource): Flow<PagingData<MediaItem>>

    /** Position of an image in [pagedItems]. */
    suspend fun indexOf(source: MediaSource, id: MediaId): Int

    suspend fun folderItemIds(bucketId: Long): List<MediaId>

    suspend fun timelineItemIds(): List<MediaId>

    /** Returns null if the image is not in the index. Reads the EXIF data from the file. */
    suspend fun imageDetails(id: MediaId): ImageDetails?
}

class DefaultMediaRepository @Inject constructor(
    private val indexStore: MediaIndexStore,
    private val timelineStore: TimelineStore,
    private val geoStore: GeoIndexStore,
    private val exifSource: ExifSource,
    private val placeNamer: PlaceNamer,
) : MediaRepository {
    override suspend fun getItem(id: MediaId): MediaItem? = indexStore.getById(id)

    override fun pagedItems(source: MediaSource): Flow<PagingData<MediaItem>> = when (source) {
        is MediaSource.Folder -> indexStore.pagedByBucket(source.bucketId, GRID_PAGING_CONFIG)
        MediaSource.Timeline -> timelineStore.paged(GRID_PAGING_CONFIG)
        is MediaSource.Map -> geoStore.pagedInBounds(source.bounds, GRID_PAGING_CONFIG)
    }

    override suspend fun indexOf(source: MediaSource, id: MediaId): Int = when (source) {
        is MediaSource.Folder -> indexStore.indexInBucket(source.bucketId, id)
        MediaSource.Timeline -> timelineStore.indexOf(id)
        is MediaSource.Map -> geoStore.indexInBounds(source.bounds, id)
    }

    override suspend fun folderItemIds(bucketId: Long): List<MediaId> = indexStore.idsInBucket(bucketId)

    override suspend fun timelineItemIds(): List<MediaId> = timelineStore.ids()

    override suspend fun imageDetails(id: MediaId): ImageDetails? {
        val item = indexStore.getById(id) ?: return null
        val details = ImageDetailsMapper.from(exifSource.read(item.contentUri), item)
        return details.copy(place = details.location?.let { placeNamer.nearest(it) })
    }

    internal companion object {
        private const val PAGE_SIZE = 120

        /**
         * A jump beyond the threshold (fast scrollbar, opening the viewer far down a folder) loads the target
         * page directly, instead of every page in between.
         */
        val GRID_PAGING_CONFIG = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = 2 * PAGE_SIZE,
            enablePlaceholders = true,
            jumpThreshold = 3 * PAGE_SIZE,
        )
    }
}
