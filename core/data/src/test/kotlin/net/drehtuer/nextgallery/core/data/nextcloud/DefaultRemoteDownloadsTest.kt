package net.drehtuer.nextgallery.core.data.nextcloud

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.nextcloud.DownloadProgress
import net.drehtuer.nextgallery.core.nextcloud.DownloadProgressTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultRemoteDownloadsTest {
    private val progress = MutableStateFlow(mapOf("https://a" to DownloadProgress(1, 2)))
    private val tracker = mockk<DownloadProgressTracker> {
        every { this@mockk.progress } returns
            this@DefaultRemoteDownloadsTest.progress
    }
    private val downloads = DefaultRemoteDownloads(tracker)

    @Test
    fun observe_runningDownload_returnsItsProgress() = runTest {
        assertEquals(RemoteDownloadProgress(1, 2), downloads.observe("https://a").first())
    }

    @Test
    fun observe_otherUrl_isNull() = runTest {
        assertNull(downloads.observe("https://b").first())
    }
}
