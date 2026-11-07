package net.drehtuer.nextgallery.core.data.index

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.fakes.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class DefaultIndexRepositoryTest {
    private val indexer = mockk<MediaIndexer>()
    private val backfill = mockk<LocationBackfill> { coEvery { run() } returns 0 }

    private fun TestScope.repository() =
        DefaultIndexRepository(indexer, backfill, TestDispatcherProvider(StandardTestDispatcher(testScheduler)))

    @Test
    fun sync_indexerSucceeds_returnsStatsAndIdleStatus() = runTest {
        coEvery { indexer.sync(IndexMode.FULL) } returns IndexStats(upserted = 3, deleted = 1)
        val repository = repository()

        val result = repository.sync(IndexMode.FULL)

        assertEquals(AppResult.Success(IndexStats(3, 1)) to IndexStatus.Idle, result to repository.status.value)
    }

    @Test
    fun sync_indexerFails_returnsFailureAndFailedStatus() = runTest {
        coEvery { indexer.sync(IndexMode.FULL) } throws IOException("gone")
        val repository = repository()

        val result = repository.sync(IndexMode.FULL)

        val error = AppError.Io("gone")
        assertEquals(AppResult.Failure(error) to IndexStatus.Failed(error), result to repository.status.value)
    }

    @Test
    fun sync_indexerSucceeds_runsLocationPass() = runTest {
        coEvery { indexer.sync(IndexMode.INCREMENTAL) } returns IndexStats.EMPTY

        repository().sync(IndexMode.INCREMENTAL)

        coVerify { backfill.run() }
    }

    @Test
    fun sync_indexerFails_skipsLocationPass() = runTest {
        coEvery { indexer.sync(IndexMode.INCREMENTAL) } throws IOException("gone")

        repository().sync(IndexMode.INCREMENTAL)

        coVerify(exactly = 0) { backfill.run() }
    }
}
