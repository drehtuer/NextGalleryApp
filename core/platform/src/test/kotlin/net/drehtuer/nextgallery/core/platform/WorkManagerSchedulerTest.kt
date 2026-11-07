package net.drehtuer.nextgallery.core.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.drehtuer.nextgallery.core.data.index.IndexMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WorkManagerSchedulerTest {
    private val workManager = mockk<WorkManager>(relaxed = true)

    private fun enqueuedRequest(mode: IndexMode): OneTimeWorkRequest {
        val request = slot<OneTimeWorkRequest>()
        WorkManagerScheduler(workManager).enqueueIndex(mode)
        verify {
            workManager.enqueueUniqueWork(
                IndexWorker.UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                capture(request),
            )
        }
        return request.captured
    }

    @Test
    fun enqueueIndex_passesModeToWorker() {
        val request = enqueuedRequest(IndexMode.INCREMENTAL)

        assertEquals("INCREMENTAL", request.workSpec.input.getString(IndexWorker.KEY_MODE))
    }

    @Test
    fun enqueueIndex_fullScan_isExpedited() {
        assertTrue(enqueuedRequest(IndexMode.FULL).workSpec.expedited)
    }

    @Test
    fun enqueueIndex_incrementalSync_isNotExpedited() {
        assertFalse(enqueuedRequest(IndexMode.INCREMENTAL).workSpec.expedited)
    }

    private fun periodicRequest(): PeriodicWorkRequest {
        val request = slot<PeriodicWorkRequest>()
        WorkManagerScheduler(workManager).scheduleUploadStateSync()
        verify {
            workManager.enqueueUniquePeriodicWork(
                "upload-state-sync",
                ExistingPeriodicWorkPolicy.UPDATE,
                capture(request),
            )
        }
        return request.captured
    }

    private fun refreshRequest(): OneTimeWorkRequest {
        val request = slot<OneTimeWorkRequest>()
        WorkManagerScheduler(workManager).requestUploadStateSync()
        verify {
            workManager.enqueueUniqueWork("upload-state-refresh", ExistingWorkPolicy.REPLACE, capture(request))
        }
        return request.captured
    }

    @Test
    fun scheduleUploadStateSync_runsEverySixHours() {
        assertEquals(TimeUnit.HOURS.toMillis(6), periodicRequest().workSpec.intervalDuration)
    }

    @Test
    fun scheduleUploadStateSync_needsANetwork() {
        assertEquals(NetworkType.CONNECTED, periodicRequest().workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun scheduleUploadStateSync_runsTheWorker() {
        assertEquals(UploadStateSyncWorker::class.java.name, periodicRequest().workSpec.workerClassName)
    }

    @Test
    fun requestUploadStateSync_needsANetwork() {
        assertEquals(NetworkType.CONNECTED, refreshRequest().workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun requestUploadStateSync_runsTheWorker() {
        assertEquals(UploadStateSyncWorker::class.java.name, refreshRequest().workSpec.workerClassName)
    }
}
