package net.drehtuer.nextgallery.core.data.index

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaChangeSource
import net.drehtuer.nextgallery.core.data.fakes.FakeSyncStateStore
import net.drehtuer.nextgallery.core.data.fakes.FakeWorkScheduler
import net.drehtuer.nextgallery.core.model.MediaAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexSchedulerTest {
    private val changes = FakeMediaChangeSource()
    private val workScheduler = FakeWorkScheduler()

    private fun scheduler(syncStateStore: FakeSyncStateStore = FakeSyncStateStore()) =
        IndexScheduler(syncStateStore, changes, workScheduler)

    @Test
    fun onPhotoLocationsAllowed_enqueuesSyncWithItsLocationPass() {
        scheduler().onPhotoLocationsAllowed()

        assertEquals(listOf(IndexMode.INCREMENTAL), workScheduler.enqueued)
    }

    @Test
    fun onAppStart_accessDenied_enqueuesNothing() = runTest {
        scheduler().onAppStart(MediaAccess.DENIED)

        assertTrue(workScheduler.enqueued.isEmpty())
    }

    @Test
    fun onAppStart_neverIndexed_enqueuesFullScan() = runTest {
        scheduler().onAppStart(MediaAccess.FULL)

        assertEquals(listOf(IndexMode.FULL), workScheduler.enqueued)
    }

    @Test
    fun onAppStart_indexedBefore_enqueuesIncrementalSync() = runTest {
        val store = FakeSyncStateStore(SyncState("external_primary", 1, "v1", 0))

        scheduler(store).onAppStart(MediaAccess.FULL)

        assertEquals(listOf(IndexMode.INCREMENTAL), workScheduler.enqueued)
    }

    @Test
    fun onAccessChanged_granted_enqueuesFullScan() {
        scheduler().onAccessChanged(MediaAccess.PARTIAL)

        assertEquals(listOf(IndexMode.FULL), workScheduler.enqueued)
    }

    @Test
    fun onAccessChanged_denied_enqueuesNothing() {
        scheduler().onAccessChanged(MediaAccess.DENIED)

        assertTrue(workScheduler.enqueued.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun observeMediaChanges_burstOfChanges_enqueuesOneIncrementalSync() = runTest {
        val job = launch { scheduler().observeMediaChanges() }
        runCurrent()

        repeat(5) { changes.events.emit(Unit) }
        advanceTimeBy(IndexScheduler.CHANGE_DEBOUNCE_MILLIS + 1)
        job.cancel()

        assertEquals(listOf(IndexMode.INCREMENTAL), workScheduler.enqueued)
    }
}
