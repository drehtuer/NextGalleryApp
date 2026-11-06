package net.drehtuer.nextgallery.core.media

import android.content.Context
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.index.ExifSource
import net.drehtuer.nextgallery.core.model.RawExif
import java.io.IOException
import javax.inject.Inject

class AndroidExifSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : ExifSource {
    override suspend fun read(contentUri: String): RawExif? = withContext(dispatchers.io) {
        try {
            context.contentResolver.openInputStream(contentUri.toUri())?.use { stream ->
                val exif = ExifInterface(stream)
                RawExif(
                    dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                    offsetTimeOriginal = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
                    subSecTimeOriginal = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL),
                )
            }
        } catch (_: IOException) {
            null
        }
    }
}
