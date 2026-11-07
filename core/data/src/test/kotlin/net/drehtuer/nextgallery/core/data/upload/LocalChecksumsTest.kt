package net.drehtuer.nextgallery.core.data.upload

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
class LocalChecksumsTest {
    private fun checksums(source: MediaContentSource) =
        LocalChecksums(source, TestDispatcherProvider(UnconfinedTestDispatcher()))

    @Test
    fun digest_readsTheContent() = runTest {
        val digest = checksums { "abc".byteInputStream() }.digest("content://1", ChecksumAlgorithm.MD5)

        assertEquals("900150983cd24fb0d6963f7d28e17f72", digest)
    }

    @Test
    fun digest_noContent_isNull() = runTest {
        assertNull(checksums { null }.digest("content://1", ChecksumAlgorithm.MD5))
    }

    @Test
    fun digest_fileGone_isNull() = runTest {
        assertNull(checksums { throw FileNotFoundException() }.digest("content://1", ChecksumAlgorithm.MD5))
    }

    @Test
    fun digest_originalRefused_isNull() = runTest {
        assertNull(checksums { throw UnsupportedOperationException() }.digest("content://1", ChecksumAlgorithm.MD5))
    }
}
