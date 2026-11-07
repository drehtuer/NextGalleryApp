package net.drehtuer.nextgallery.core.data.index

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.model.DayCount
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem

/** Port: the local image index by date. Implemented with Room in `:core:database`. */
interface TimelineStore {
    /** All images by local date, newest day first and newest first within a day (D7). */
    fun paged(config: PagingConfig): Flow<PagingData<MediaItem>>

    /** Ids of all images, in the order of [paged]. */
    suspend fun ids(): List<MediaId>

    /** Position of an image in [paged]. */
    suspend fun indexOf(id: MediaId): Int

    /** The number of images per day, newest day first. */
    fun observeDayCounts(): Flow<List<DayCount>>
}
