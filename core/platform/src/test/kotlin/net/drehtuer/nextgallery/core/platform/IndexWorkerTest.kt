package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.IndexRepository
import net.drehtuer.nextgallery.core.data.index.IndexStats
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class IndexWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = mockk<IndexRepository>()

    private fun worker(mode: IndexMode?) = TestListenableWorkerBuilder<IndexWorker>(context)
        .setInputData(mode?.let { workDataOf(IndexWorker.KEY_MODE to it.name) } ?: workDataOf())
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = IndexWorker(appContext, workerParameters, repository)
            },
        )
        .build()

    @Test
    fun doWork_syncSucceeds_returnsSuccess() = runTest {
        coEvery { repository.sync(IndexMode.FULL) } returns AppResult.Success(IndexStats.EMPTY)

        assertEquals(ListenableWorker.Result.success(), worker(IndexMode.FULL).doWork())
    }

    @Test
    fun doWork_syncFailsWithIoError_returnsRetry() = runTest {
        coEvery { repository.sync(IndexMode.FULL) } returns AppResult.Failure(AppError.from(IOException()))

        assertEquals(ListenableWorker.Result.retry(), worker(IndexMode.FULL).doWork())
    }

    @Test
    fun doWork_permissionDenied_returnsFailure() = runTest {
        coEvery { repository.sync(IndexMode.FULL) } returns AppResult.Failure(AppError.PermissionDenied)

        assertEquals(ListenableWorker.Result.failure(), worker(IndexMode.FULL).doWork())
    }

    @Test
    fun doWork_noModeGiven_runsIncrementalSync() = runTest {
        coEvery { repository.sync(IndexMode.INCREMENTAL) } returns AppResult.Success(IndexStats.EMPTY)

        worker(mode = null).doWork()

        coVerify { repository.sync(IndexMode.INCREMENTAL) }
    }
}
