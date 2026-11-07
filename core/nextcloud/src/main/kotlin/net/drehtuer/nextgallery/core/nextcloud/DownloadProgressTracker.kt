package net.drehtuer.nextgallery.core.nextcloud

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

data class DownloadProgress(val bytesRead: Long, val totalBytes: Long)

/** Reports how far the downloads of original files are, per URL, for "Loading full image · 2.4 of 6.1 MB". */
class DownloadProgressTracker : Interceptor {
    private val mutableProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())

    val progress: StateFlow<Map<String, DownloadProgress>> = mutableProgress

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val url = chain.request().url
        if (!response.isSuccessful || chain.request().method != "GET" || DAV_FILES !in url.encodedPath) return response
        return response.newBuilder().body(TrackedBody(response.body, url.toString())).build()
    }

    private fun report(url: String, bytesRead: Long, total: Long) {
        mutableProgress.update { it + (url to DownloadProgress(bytesRead, total)) }
    }

    private fun finish(url: String) {
        mutableProgress.update { it - url }
    }

    private inner class TrackedBody(private val body: ResponseBody, private val url: String) : ResponseBody() {
        private val tracked: BufferedSource by lazy { TrackedSource(body.source()).buffer() }

        override fun contentLength(): Long = body.contentLength()

        override fun contentType(): MediaType? = body.contentType()

        override fun source(): BufferedSource = tracked

        private inner class TrackedSource(source: Source) : ForwardingSource(source) {
            private var bytesRead = 0L
            private var reported = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                if (read > 0) bytesRead += read
                if (bytesRead - reported >= REPORT_STEP_BYTES) {
                    reported = bytesRead
                    report(url, bytesRead, body.contentLength())
                }
                return read
            }

            override fun close() {
                finish(url)
                super.close()
            }
        }
    }

    private companion object {
        const val DAV_FILES = "/remote.php/dav/files/"
        const val REPORT_STEP_BYTES = 128L * 1024
    }
}
