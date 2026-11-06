package net.drehtuer.nextgallery.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class RunCatchingCancellableTest {
    @Test
    fun runCatchingCancellable_blockSucceeds_returnsSuccess() {
        assertEquals(AppResult.Success(42), runCatchingCancellable { 42 })
    }

    @Test
    fun runCatchingCancellable_blockThrows_returnsFailureWithMappedError() {
        val result = runCatchingCancellable { throw IOException("disk") }

        assertEquals(AppResult.Failure(AppError.Io("disk")), result)
    }

    @Test(expected = CancellationException::class)
    fun runCatchingCancellable_blockCancelled_rethrows() {
        runCatchingCancellable { throw CancellationException() }
    }

    @Test
    fun runCatchingCancellable_blockThrowsError_propagatesError() {
        val thrown = runCatching { runCatchingCancellable { throw OutOfMemoryError() } }.exceptionOrNull()

        assertTrue(thrown is OutOfMemoryError)
    }
}
