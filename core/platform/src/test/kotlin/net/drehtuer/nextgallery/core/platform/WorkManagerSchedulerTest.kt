package net.drehtuer.nextgallery.core.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
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
}
