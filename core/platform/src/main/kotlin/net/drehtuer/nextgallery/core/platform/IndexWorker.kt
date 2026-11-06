package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.IndexRepository

/** Runs one index sync; all logic is in [IndexRepository]. */
@HiltWorker
class IndexWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val indexRepository: IndexRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val mode = inputData.getString(KEY_MODE)?.let(IndexMode::valueOf) ?: IndexMode.INCREMENTAL
        return when (val result = indexRepository.sync(mode)) {
            is AppResult.Success -> Result.success()

            // Retrying cannot help without permission; the next app start or access change schedules a new sync.
            is AppResult.Failure -> if (result.error == AppError.PermissionDenied) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val KEY_MODE = "mode"
        const val UNIQUE_WORK_NAME = "index"
    }
}
