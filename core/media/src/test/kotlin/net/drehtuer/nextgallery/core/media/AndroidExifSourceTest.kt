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
    fun read_missingFile_returnsNull() = runTest {
        val source = AndroidExifSource(context, testDispatchers())

        assertNull(source.read(Uri.fromFile(File(context.cacheDir, "missing.jpg")).toString()))
    }

    private fun kotlinx.coroutines.test.TestScope.testDispatchers() = object : DispatcherProvider {
        override val io = StandardTestDispatcher(testScheduler)
        override val default = io
    }
}
