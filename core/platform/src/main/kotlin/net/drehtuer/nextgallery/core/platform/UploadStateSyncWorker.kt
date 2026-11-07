package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.upload.UploadStateRepository

/** Runs one upload-state check; all logic is in [UploadStateRepository]. */
@HiltWorker
class UploadStateSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: UploadStateRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (val result = repository.refreshAll()) {
        is AppResult.Success -> Result.success()

        // Other errors, such as a rejected password, need the user; the next periodic run looks again.
        is AppResult.Failure -> when (result.error) {
            is AppError.Network, is AppError.Io, is AppError.Server -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "upload-state-sync"
        const val REFRESH_WORK_NAME = "upload-state-refresh"
    }
}
