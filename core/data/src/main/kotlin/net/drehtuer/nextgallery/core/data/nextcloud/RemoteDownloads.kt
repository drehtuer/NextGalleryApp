package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.nextcloud.DownloadProgressTracker
import javax.inject.Inject

/** How far an original image is, for "Loading full image · 2.4 of 6.1 MB". */
data class RemoteDownloadProgress(val bytesRead: Long, val totalBytes: Long)

interface RemoteDownloads {
    /** Null while nothing is being downloaded from [url]. */
    fun observe(url: String): Flow<RemoteDownloadProgress?>
}

class DefaultRemoteDownloads @Inject constructor(private val tracker: DownloadProgressTracker) : RemoteDownloads {
    override fun observe(url: String): Flow<RemoteDownloadProgress?> = tracker.progress
        .map { all -> all[url]?.let { RemoteDownloadProgress(it.bytesRead, it.totalBytes) } }
        .distinctUntilChanged()
}
