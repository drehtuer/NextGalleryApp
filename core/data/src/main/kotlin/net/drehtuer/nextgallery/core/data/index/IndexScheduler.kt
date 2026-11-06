package net.drehtuer.nextgallery.core.data.index

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import net.drehtuer.nextgallery.core.model.MediaAccess
import javax.inject.Inject

/** Decides when and how the index is synced; the work itself runs in a background worker. */
class IndexScheduler @Inject constructor(
    private val syncStateStore: SyncStateStore,
    private val mediaChangeSource: MediaChangeSource,
    private val workScheduler: WorkScheduler,
) {
    suspend fun onAppStart(access: MediaAccess) {
        if (access == MediaAccess.DENIED) return
        workScheduler.enqueueIndex(if (syncStateStore.hasAny()) IndexMode.INCREMENTAL else IndexMode.FULL)
    }

    /** A new grant or a changed partial selection can add or remove images, so the index is rebuilt. */
    fun onAccessChanged(access: MediaAccess) {
        if (access == MediaAccess.DENIED) return
        workScheduler.enqueueIndex(IndexMode.FULL)
    }

    @OptIn(FlowPreview::class)
    suspend fun observeMediaChanges() {
        mediaChangeSource.changes()
            .debounce(CHANGE_DEBOUNCE_MILLIS)
            .collect { workScheduler.enqueueIndex(IndexMode.INCREMENTAL) }
    }

    companion object {
        const val CHANGE_DEBOUNCE_MILLIS = 2_000L
    }
}
