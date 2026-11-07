package net.drehtuer.nextgallery.core.platform

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.upload.MediaContentSource
import java.io.InputStream
import javax.inject.Inject

class ContentResolverContentSource @Inject constructor(@ApplicationContext private val context: Context) :
    MediaContentSource {
    /**
     * Asks for the original: without it, the system blanks the location in the file, and the checksum would not match
     * the server's. Without `ACCESS_MEDIA_LOCATION` this throws, which leaves a match unconfirmed.
     */
    override fun open(contentUri: String): InputStream? =
        context.contentResolver.openInputStream(MediaStore.setRequireOriginal(contentUri.toUri()))
}
