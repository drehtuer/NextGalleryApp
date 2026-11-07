package net.drehtuer.nextgallery.core.media

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** `setRequireOriginal` against the real MediaStore (ARCHITECTURE.md §13.1). */
@RunWith(AndroidJUnit4::class)
class AndroidExifSourceGpsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dispatchers = object : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
    }
    private lateinit var inserted: Uri

    @Before
    fun insertGeotaggedImage() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_MEDIA_LOCATION)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "nextgallery-gps-test-${System.nanoTime()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NextGalleryTest")
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        inserted = context.contentResolver.insert(collection, values)!!
        context.contentResolver.openOutputStream(inserted)!!.use {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        context.contentResolver.openFileDescriptor(inserted, "rw")!!.use { descriptor: ParcelFileDescriptor ->
            ExifInterface(descriptor.fileDescriptor).apply {
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "45/1,30/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "10/1,15/1,0/1")
                setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
                saveAttributes()
            }
        }
    }

    @After
    fun deleteImage() {
        context.contentResolver.delete(inserted, null, null)
    }

    @Test
    fun read_permissionGranted_returnsGpsTags() = runBlocking {
        val exif = AndroidExifSource(context, dispatchers).read(inserted.toString())

        assertEquals("N", exif?.gpsLatitudeRef)
        assertEquals("E", exif?.gpsLongitudeRef)
    }
}
