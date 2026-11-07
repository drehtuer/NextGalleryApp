package net.drehtuer.nextgallery.core.platform

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/** `setRequireOriginal` against the real MediaStore (ARCHITECTURE.md §13.1). */
@RunWith(AndroidJUnit4::class)
class ContentResolverContentSourceDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var inserted: Uri
    private lateinit var written: ByteArray

    @Before
    fun insertImage() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_MEDIA_LOCATION)
        written = ByteArrayOutputStream().also {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }.toByteArray()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "nextgallery-content-test-${System.nanoTime()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NextGalleryTest")
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        inserted = context.contentResolver.insert(collection, values)!!
        context.contentResolver.openOutputStream(inserted)!!.use { it.write(written) }
    }

    @After
    fun deleteImage() {
        context.contentResolver.delete(inserted, null, null)
    }

    @Test
    fun open_permissionGranted_readsTheBytesOfTheFile() {
        val bytes = ContentResolverContentSource(context).open(inserted.toString())!!.use { it.readBytes() }

        assertArrayEquals(written, bytes)
    }
}
