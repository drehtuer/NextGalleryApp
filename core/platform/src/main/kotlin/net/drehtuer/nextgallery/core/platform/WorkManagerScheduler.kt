package net.drehtuer.nextgallery.core.platform

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import java.util.concurrent.TimeUnit
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

    override fun scheduleUploadStateSync() {
        val request = PeriodicWorkRequestBuilder<UploadStateSyncWorker>(UPLOAD_STATE_SYNC_HOURS, TimeUnit.HOURS)
            .setConstraints(NETWORK_CONNECTED)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UploadStateSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Replaces a check that is already running, which may have missed what the request is about. */
    override fun requestUploadStateSync() {
        val request = OneTimeWorkRequestBuilder<UploadStateSyncWorker>().setConstraints(NETWORK_CONNECTED).build()
        workManager.enqueueUniqueWork(UploadStateSyncWorker.REFRESH_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val UPLOAD_STATE_SYNC_HOURS = 6L
        val NETWORK_CONNECTED = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }
}
