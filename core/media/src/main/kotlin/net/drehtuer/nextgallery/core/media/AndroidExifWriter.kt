package net.drehtuer.nextgallery.core.media

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.edit.EncodedImage
import net.drehtuer.nextgallery.core.data.edit.ExifWriter
import java.io.IOException
import javax.inject.Inject

class AndroidExifWriter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : ExifWriter {
    override suspend fun readTags(contentUri: String, tags: Set<String>): Map<String, String> =
        withContext(dispatchers.io) {
            val stream = context.contentResolver.openInputStream(originalUri(contentUri.toUri()))
                ?: throw IOException("Cannot open the image")
            stream.use {
                val exif = ExifInterface(it)
                tags.mapNotNull { tag -> exif.getAttribute(tag)?.let { value -> tag to value } }.toMap()
            }
        }

    override suspend fun writeTags(image: EncodedImage, tags: Map<String, String>) = withContext(dispatchers.io) {
        val exif = ExifInterface(image.file)
        tags.forEach { (tag, value) -> exif.setAttribute(tag, value) }
        exif.saveAttributes()
    }

    /** MediaStore removes the location from a normal stream, and the copy must keep it. */
    private fun originalUri(uri: Uri): Uri {
        val mayReadLocation = context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return if (mayReadLocation && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            MediaStore.setRequireOriginal(uri)
        } else {
            uri
        }
    }
}
