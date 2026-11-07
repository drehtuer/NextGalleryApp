package net.drehtuer.nextgallery.core.ui.thumbnail

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.os.CancellationSignal
import android.util.Size
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import net.drehtuer.nextgallery.core.model.MediaItem
import java.io.IOException

/** Coil model for a local grid thumbnail. */
data class MediaThumbnail(val id: Long, val contentUri: String, val dateModifiedSeconds: Long)

fun MediaItem.toThumbnail() = MediaThumbnail(id.value, contentUri, dateModifiedSeconds)

private const val SMALL_THUMBNAIL_PX = 256
const val LARGE_THUMBNAIL_PX = 512

/** Fixed sizes instead of exact cell sizes, so that different grid densities share cached thumbnails. */
fun thumbnailSizePx(gridWidthPx: Int, columns: Int): Int =
    if (gridWidthPx / columns.coerceAtLeast(1) <= SMALL_THUMBNAIL_PX) SMALL_THUMBNAIL_PX else LARGE_THUMBNAIL_PX

/** An edited image has a new modification date, so its thumbnail gets a new key. */
class MediaThumbnailKeyer : Keyer<MediaThumbnail> {
    override fun key(data: MediaThumbnail, options: Options): String {
        val size = options.size.width.pxOrElse { 0 }
        return "media:${data.id}:${data.dateModifiedSeconds}:$size"
    }
}

/** Loads thumbnails from the system's thumbnail cache, which is faster than decoding the originals. */
class MediaThumbnailFetcher(
    private val data: MediaThumbnail,
    private val options: Options,
    private val contentResolver: ContentResolver,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val uri = data.contentUri.toUri()
        val side = options.size.width.pxOrElse { LARGE_THUMBNAIL_PX }
        val signal = CancellationSignal()
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { signal.cancel() }
        val bitmap = try {
            contentResolver.loadThumbnail(uri, Size(side, side), signal)
        } catch (_: IOException) {
            // The system cannot create a thumbnail for every file; decoding the original still may work.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                val scale = side.toFloat() / maxOf(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }
        } finally {
            cancellation.dispose()
        }
        return ImageFetchResult(bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory(private val contentResolver: ContentResolver) : Fetcher.Factory<MediaThumbnail> {
        override fun create(data: MediaThumbnail, options: Options, imageLoader: ImageLoader): Fetcher =
            MediaThumbnailFetcher(data, options, contentResolver)
    }
}
