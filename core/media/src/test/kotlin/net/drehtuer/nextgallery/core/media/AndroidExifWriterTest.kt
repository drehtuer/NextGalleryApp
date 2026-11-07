package net.drehtuer.nextgallery.core.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.edit.EncodedImage
import net.drehtuer.nextgallery.core.data.edit.ExifPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidExifWriterTest {
    /** Serves one file and notes how it was asked for. */
    class FileProvider : ContentProvider() {
        override fun onCreate() = true

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0

        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            args: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            openedUri = uri
            return ParcelFileDescriptor.open(providedFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }

    private companion object {
        const val AUTHORITY = "test.exif"
        var providedFile: File? = null
        var openedUri: Uri? = null
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun TestScope.writer() = AndroidExifWriter(
        context,
        object : DispatcherProvider {
            override val io = StandardTestDispatcher(testScheduler)
            override val default = io
        },
    )

    private fun image(name: String, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): File {
        val file = File(context.cacheDir, name)
        file.outputStream().use { Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888).compress(format, 90, it) }
        return file
    }

    private fun source(): File = image("source.jpg").also { file ->
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_MAKE, "Google")
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2024:03:15 14:30:05")
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setLatLong(48.0, 11.0)
            saveAttributes()
        }
    }

    @Test
    fun readTags_returnsRequestedTagsThatAreSet() = runTest {
        val tags = writer().readTags(Uri.fromFile(source()).toString(), setOf("Make", "Model"))

        assertEquals(mapOf("Make" to "Google"), tags)
    }

    @Test(expected = IOException::class)
    fun readTags_missingFile_throwsIoException() = runTest {
        writer().readTags(Uri.fromFile(File(context.cacheDir, "missing.jpg")).toString(), setOf("Make"))
    }

    @Test
    fun readTags_withLocationPermissionOnFileUri_stillReads() = runTest {
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.ACCESS_MEDIA_LOCATION)

        val tags = writer().readTags(Uri.fromFile(source()).toString(), setOf("Make"))

        assertEquals(mapOf("Make" to "Google"), tags)
    }

    @Test
    fun readTags_contentUriWithLocationPermission_asksForTheOriginal() = runTest {
        providedFile = source()
        Robolectric.setupContentProvider(FileProvider::class.java, AUTHORITY)
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.ACCESS_MEDIA_LOCATION)

        writer().readTags("content://$AUTHORITY/1", setOf("Make"))

        assertEquals("1", openedUri?.getQueryParameter("requireOriginal"))
    }

    @Test
    fun readTags_contentUriWithoutLocationPermission_readsTheNormalStream() = runTest {
        providedFile = source()
        Robolectric.setupContentProvider(FileProvider::class.java, AUTHORITY)
        shadowOf(context as android.app.Application).denyPermissions(android.Manifest.permission.ACCESS_MEDIA_LOCATION)

        writer().readTags("content://$AUTHORITY/1", setOf("Make"))

        assertNull(openedUri?.getQueryParameter("requireOriginal"))
    }

    @Test
    fun writeTags_jpeg_appliesThePolicy() = runTest {
        val writer = writer()
        val output = image("output.jpg")
        val read = writer.readTags(Uri.fromFile(source()).toString(), ExifPolicy.COPIED_TAGS + "Orientation")

        writer.writeTags(EncodedImage(output), ExifPolicy.forOutput(read, 8, 4))

        val exif = ExifInterface(output)
        assertEquals("Google", exif.getAttribute(ExifInterface.TAG_MAKE))
        assertEquals("2024:03:15 14:30:05", exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertEquals(ExifInterface.ORIENTATION_NORMAL.toString(), exif.getAttribute(ExifInterface.TAG_ORIENTATION))
        assertEquals("8", exif.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION))
        assertEquals("4", exif.getAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION))
    }

    @Test
    fun writeTags_jpeg_keepsTheLocation() = runTest {
        val writer = writer()
        val output = image("output.jpg")
        val read = writer.readTags(Uri.fromFile(source()).toString(), ExifPolicy.COPIED_TAGS)

        writer.writeTags(EncodedImage(output), ExifPolicy.forOutput(read, 8, 4))

        assertEquals(48.0, ExifInterface(output).latLong!![0], 1e-3)
    }

    @Test
    fun writeTags_png_isSaved() = runTest {
        val output = image("output.png", Bitmap.CompressFormat.PNG)

        writer().writeTags(EncodedImage(output), mapOf("Make" to "Google"))

        assertEquals("Google", ExifInterface(output).getAttribute(ExifInterface.TAG_MAKE))
    }
}
