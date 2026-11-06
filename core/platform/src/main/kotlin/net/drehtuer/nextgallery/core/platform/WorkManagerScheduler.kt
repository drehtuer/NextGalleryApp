package net.drehtuer.nextgallery.core.platform

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import javax.inject.Inject

class WorkManagerScheduler @Inject constructor(private val workManager: WorkManager) : WorkScheduler {
    /**
     * Requests are appended to the unique `index` chain, so a sync triggered while another runs is not lost. The
     * first full scan is expedited, so folders appear quickly; it falls back to normal work when out of quota.
     */
    override fun enqueueIndex(mode: IndexMode) {
        val request = OneTimeWorkRequestBuilder<IndexWorker>()
            .setInputData(workDataOf(IndexWorker.KEY_MODE to mode.name))
            .apply { if (mode == IndexMode.FULL) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) }
            .build()
        workManager.enqueueUniqueWork(IndexWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
