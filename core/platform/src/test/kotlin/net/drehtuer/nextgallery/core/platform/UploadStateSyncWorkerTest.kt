package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.upload.UploadStateRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadStateSyncWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = mockk<UploadStateRepository>()

    private fun worker() = TestListenableWorkerBuilder<UploadStateSyncWorker>(context)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = UploadStateSyncWorker(appContext, workerParameters, repository)
            },
        )
        .build()

    private suspend fun resultFor(error: AppError): ListenableWorker.Result {
        coEvery { repository.refreshAll() } returns AppResult.Failure(error)
        return worker().doWork()
    }

    @Test
    fun doWork_success_returnsSuccess() = runTest {
        coEvery { repository.refreshAll() } returns AppResult.Success(Unit)

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
    }

    @Test
    fun doWork_offline_returnsRetry() = runTest {
        assertEquals(ListenableWorker.Result.retry(), resultFor(AppError.Network("offline")))
    }

    @Test
    fun doWork_localReadError_returnsRetry() = runTest {
        assertEquals(ListenableWorker.Result.retry(), resultFor(AppError.Io("disk")))
    }

    @Test
    fun doWork_serverError_returnsRetry() = runTest {
        assertEquals(ListenableWorker.Result.retry(), resultFor(AppError.Server(503)))
    }

    @Test
    fun doWork_passwordRejected_returnsFailure() = runTest {
        assertEquals(ListenableWorker.Result.failure(), resultFor(AppError.Auth))
    }
}
