package net.drehtuer.nextgallery.core.data.timeline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.data.index.TimelineStore
import javax.inject.Inject

interface TimelineRepository {
    /** The timeline's layout. It only emits when the number of images of a day changes. */
    fun observeIndex(): Flow<TimelineIndex>
}

class DefaultTimelineRepository @Inject constructor(private val store: TimelineStore) : TimelineRepository {
    override fun observeIndex(): Flow<TimelineIndex> =
        store.observeDayCounts().distinctUntilChanged().map { TimelineIndex(it) }
}
