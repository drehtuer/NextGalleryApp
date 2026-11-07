package net.drehtuer.nextgallery.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
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
            context.contentResolver.openInputStream(originalOf(contentUri.toUri()))?.use { stream ->
                val exif = ExifInterface(stream)
                RawExif(
                    dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                    offsetTimeOriginal = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
                    subSecTimeOriginal = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL),
                    make = exif.getAttribute(ExifInterface.TAG_MAKE),
                    model = exif.getAttribute(ExifInterface.TAG_MODEL),
                    exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                    fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY),
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
                    gpsLatitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
                    gpsLatitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
                    gpsLongitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
                    gpsLongitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF),
                )
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: UnsupportedOperationException) {
            // setRequireOriginal: the permission was revoked between the check and the read.
            null
        }
    }

    /** Android replaces the location in a file with zeros unless the app asks for the original (D16). */
    private fun originalOf(uri: Uri): Uri {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return if (granted) MediaStore.setRequireOriginal(uri) else uri
    }
}
