package net.drehtuer.nextgallery.core.nextcloud

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadProgressTrackerTest {
    private val server = MockWebServer()
    private val tracker = DownloadProgressTracker()
    private val client = OkHttpClient.Builder().addNetworkInterceptor(tracker).build()
    private val body = Buffer().write(ByteArray(512 * 1024))

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun whileReadingAnOriginal_reportsProgress() {
        server.enqueue(MockResponse().setBody(body))
        val url = server.url("/remote.php/dav/files/anna/a.jpg")

        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            response.body.source().readByteArray(256 * 1024)

            val progress = tracker.progress.value.getValue(url.toString())
            assertEquals(512L * 1024, progress.totalBytes)
            assertTrue(progress.bytesRead >= 128L * 1024)
        }
    }

    @Test
    fun afterClosing_removesTheEntry() {
        server.enqueue(MockResponse().setBody(body))
        val url = server.url("/remote.php/dav/files/anna/a.jpg")

        client.newCall(Request.Builder().url(url).build()).execute().use { it.body.bytes() }

        assertTrue(tracker.progress.value.isEmpty())
    }

    @Test
    fun otherRequests_areNotTracked() {
        server.enqueue(MockResponse().setBody(body))
        val url = server.url("/index.php/core/preview")

        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            response.body.source().readByteArray(256 * 1024)

            assertTrue(tracker.progress.value.isEmpty())
        }
    }
}
