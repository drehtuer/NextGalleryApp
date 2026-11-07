package net.drehtuer.nextgallery.core.database

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.data.index.TimelineStore
import net.drehtuer.nextgallery.core.model.DayCount
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import javax.inject.Inject

class RoomTimelineStore @Inject constructor(private val dao: TimelineDao) : TimelineStore {
    override fun paged(config: PagingConfig): Flow<PagingData<MediaItem>> =
        Pager(config) { dao.paging() }.flow.map { page -> page.map { it.toModel() } }

    override suspend fun ids(): List<MediaId> = dao.ids().map { MediaId(it) }

    override suspend fun indexOf(id: MediaId): Int = dao.indexOf(id.value)

    override fun observeDayCounts(): Flow<List<DayCount>> =
        dao.observeDayCounts().map { rows -> rows.map { it.toModel() } }
}
