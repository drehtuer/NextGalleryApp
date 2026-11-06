package net.drehtuer.nextgallery.core.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.model.RawExif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidExifSourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun jpegWithExif(): File {
        val file = File(context.cacheDir, "exif.jpg")
        file.outputStream().use {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2024:03:15 14:30:05")
            setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, "+02:00")
            setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, "25")
            saveAttributes()
        }
        return file
    }

    @Test
    fun read_jpegWithDateTags_returnsRawValues() = runTest {
        val source = AndroidExifSource(context, testDispatchers())

        val exif = source.read(Uri.fromFile(jpegWithExif()).toString())

        assertEquals(RawExif("2024:03:15 14:30:05", "+02:00", "25"), exif)
    }

    @Test
    fun read_jpegWithCameraTags_returnsRawValues() = runTest {
        val source = AndroidExifSource(context, testDispatchers())
        val file = jpegWithExif()
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_MAKE, "Google")
            setAttribute(ExifInterface.TAG_MODEL, "Pixel 10a")
            setAttribute(ExifInterface.TAG_EXPOSURE_TIME, "0.008")
            setAttribute(ExifInterface.TAG_F_NUMBER, "1.8")
            setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, "400")
            setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "27/5")
            saveAttributes()
        }

        val exif = source.read(Uri.fromFile(file).toString())

        assertEquals(
            listOf("Google", "Pixel 10a", "0.008", "1.8", "400", "27/5"),
            listOf(exif?.make, exif?.model, exif?.exposureTime, exif?.fNumber, exif?.iso, exif?.focalLength),
        )
    }

    @Test
    fun read_missingFile_returnsNull() = runTest {
        val source = AndroidExifSource(context, testDispatchers())

        assertNull(source.read(Uri.fromFile(File(context.cacheDir, "missing.jpg")).toString()))
    }

    private fun kotlinx.coroutines.test.TestScope.testDispatchers() = object : DispatcherProvider {
        override val io = StandardTestDispatcher(testScheduler)
        override val default = io
    }
}
