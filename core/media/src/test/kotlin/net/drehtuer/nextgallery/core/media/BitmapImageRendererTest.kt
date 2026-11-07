package net.drehtuer.nextgallery.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.edit.EditGeometry
import net.drehtuer.nextgallery.core.data.edit.ImageFormat
import net.drehtuer.nextgallery.core.data.edit.RenderOutOfMemoryException
import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.NormalizedPoint
import net.drehtuer.nextgallery.core.model.edit.NormalizedRect
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapImageRendererTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun TestScope.renderer() = BitmapImageRenderer(
        context,
        object : DispatcherProvider {
            override val io = StandardTestDispatcher(testScheduler)
            override val default = io
        },
    )

    /** 40×20, left half red and right half blue. */
    private fun twoColors(width: Int = 40, height: Int = 20): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint()
            paint.color = Color.RED
            canvas.drawRect(0f, 0f, width / 2f, height.toFloat(), paint)
            paint.color = Color.BLUE
            canvas.drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), paint)
        }

    private fun fileOf(bitmap: Bitmap, format: Bitmap.CompressFormat, name: String): File {
        val file = File(context.cacheDir, name)
        file.outputStream().use { bitmap.compress(format, 100, it) }
        return file
    }

    private fun jpegUri(orientation: Int? = null): String {
        val file = fileOf(twoColors(), Bitmap.CompressFormat.JPEG, "source.jpg")
        if (orientation != null) {
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
        }
        return Uri.fromFile(file).toString()
    }

    private fun preview(bitmap: Bitmap) = PreviewImage(bitmap, bitmap.width, bitmap.height)

    private fun PreviewImage.pixel(x: Int, y: Int) = (handle as Bitmap).getPixel(x, y)

    private fun assertColor(expected: Int, actual: Int) {
        val close = abs(Color.red(expected) - Color.red(actual)) <= TOLERANCE &&
            abs(Color.green(expected) - Color.green(actual)) <= TOLERANCE &&
            abs(Color.blue(expected) - Color.blue(actual)) <= TOLERANCE
        assertTrue("expected ${Integer.toHexString(expected)}, was ${Integer.toHexString(actual)}", close)
    }

    @Test
    fun readSize_returnsSizeOfFile() = runTest {
        assertEquals(ImageSize(40, 20), renderer().readSize(jpegUri()))
    }

    @Test
    fun readSize_rotatedByExif_returnsUprightSize() = runTest {
        assertEquals(ImageSize(20, 40), renderer().readSize(jpegUri(ExifInterface.ORIENTATION_ROTATE_90)))
    }

    @Test(expected = IOException::class)
    fun readSize_missingFile_throwsIoException() = runTest {
        renderer().readSize(Uri.fromFile(File(context.cacheDir, "missing.jpg")).toString())
    }

    @Test
    fun decode_fullSize_hasFullSize() = runTest {
        val image = renderer().decode(jpegUri(), 1)

        assertEquals(listOf(40, 20), listOf(image.width, image.height))
    }

    @Test
    fun decode_sampled_isSmaller() = runTest {
        val image = renderer().decode(jpegUri(), 2)

        assertEquals(listOf(20, 10), listOf(image.width, image.height))
    }

    @Test
    fun decode_returnsSoftwareBitmap() = runTest {
        val bitmap = renderer().decode(jpegUri(), 1).handle as Bitmap

        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun decode_rotatedByExif_hasUprightPixels() = runTest {
        // Turned clockwise, the red left half is on top.
        val image = renderer().decode(jpegUri(ExifInterface.ORIENTATION_ROTATE_90), 1)

        assertColor(Color.RED, image.pixel(10, 5))
        assertColor(Color.BLUE, image.pixel(10, 35))
    }

    @Test(expected = IOException::class)
    fun decode_corruptFile_throwsIoException() = runTest {
        val file = File(context.cacheDir, "corrupt.jpg").apply { writeText("no image") }

        renderer().decode(Uri.fromFile(file).toString(), 1)
    }

    @Test
    fun render_noOperations_copiesPixels() = runTest {
        val plan = EditGeometry.steps(emptyList(), ImageSize(40, 20))

        val output = renderer().render(preview(twoColors()), plan)

        assertColor(Color.RED, output.pixel(5, 10))
        assertColor(Color.BLUE, output.pixel(35, 10))
    }

    @Test
    fun render_rotate_appliesMatrix() = runTest {
        val plan = EditGeometry.steps(listOf(EditOperation.Rotate(1)), ImageSize(40, 20))

        val output = renderer().render(preview(twoColors()), plan)

        assertEquals(listOf(20, 40), listOf(output.width, output.height))
        assertColor(Color.RED, output.pixel(10, 5))
        assertColor(Color.BLUE, output.pixel(10, 35))
    }

    @Test
    fun render_crop_keepsOnlyTheFrame() = runTest {
        val plan = EditGeometry.steps(
            listOf(EditOperation.Crop(NormalizedRect(0.5f, 0f, 1f, 1f))),
            ImageSize(40, 20),
        )

        val output = renderer().render(preview(twoColors()), plan)

        assertEquals(listOf(20, 20), listOf(output.width, output.height))
        assertColor(Color.BLUE, output.pixel(0, 10))
        assertColor(Color.BLUE, output.pixel(19, 10))
    }

    @Test
    fun render_stroke_drawsOverTheImage() = runTest {
        val stroke = EditOperation.PenStroke(
            listOf(NormalizedPoint(0.1f, 0.5f), NormalizedPoint(0.4f, 0.5f)),
            Color.GREEN,
            0.2f,
        )
        val plan = EditGeometry.steps(listOf(stroke), ImageSize(40, 20))

        val output = renderer().render(preview(twoColors()), plan)

        assertColor(Color.GREEN, output.pixel(10, 10))
        assertColor(Color.RED, output.pixel(10, 1))
    }

    @Test
    fun render_singlePointStroke_drawsADot() = runTest {
        val stroke = EditOperation.PenStroke(listOf(NormalizedPoint(0.25f, 0.5f)), Color.GREEN, 0.2f)
        val plan = EditGeometry.steps(listOf(stroke), ImageSize(40, 20))

        val output = renderer().render(preview(twoColors()), plan)

        assertColor(Color.GREEN, output.pixel(10, 10))
    }

    @Test
    fun render_strokeWithoutPoints_drawsNothing() = runTest {
        val stroke = EditOperation.PenStroke(emptyList(), Color.GREEN, 0.2f)
        val plan = EditGeometry.steps(listOf(stroke), ImageSize(40, 20))

        val output = renderer().render(preview(twoColors()), plan)

        assertColor(Color.RED, output.pixel(10, 10))
    }

    @Test
    fun render_downsampledBase_fillsTheOutput() = runTest {
        // The plan is for 40×20; the base is half of that.
        val plan = EditGeometry.steps(emptyList(), ImageSize(40, 20))

        val output = renderer().render(preview(twoColors(20, 10)), plan)

        assertEquals(listOf(40, 20), listOf(output.width, output.height))
        assertColor(Color.RED, output.pixel(5, 10))
        assertColor(Color.BLUE, output.pixel(35, 10))
    }

    @Test
    fun render_allOperationsTogether_composeOnRealPixels() = runTest {
        // Green stroke across the red half; turned, so red is on top; bottom half cropped away; scaled by half.
        val operations = listOf(
            EditOperation.PenStroke(
                listOf(NormalizedPoint(0.25f, 0f), NormalizedPoint(0.25f, 1f)),
                Color.GREEN,
                0.1f,
            ),
            EditOperation.Rotate(1),
            EditOperation.Crop(NormalizedRect(0f, 0f, 1f, 0.5f)),
            EditOperation.Scale(10, 10),
        )
        val plan = EditGeometry.steps(operations, ImageSize(40, 20))

        val output = renderer().render(preview(twoColors()), plan)

        assertEquals(listOf(10, 10), listOf(output.width, output.height))
        assertColor(Color.RED, output.pixel(5, 1))
        assertColor(Color.GREEN, output.pixel(5, 5))
        assertColor(Color.RED, output.pixel(5, 9))
    }

    @Test
    fun render_leavesBaseUnchanged() = runTest {
        val base = twoColors()
        val plan = EditGeometry.steps(listOf(EditOperation.Rotate(1)), ImageSize(40, 20))

        renderer().render(preview(base), plan)

        assertEquals(listOf(40, 20), listOf(base.width, base.height))
        assertFalse(base.isRecycled)
    }

    @Test
    fun encode_jpeg_writesJpeg() = runTest {
        val encoded = renderer().encode(preview(twoColors()), ImageFormat.JPEG)

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(encoded.file.path, options)
        assertEquals("image/jpeg", options.outMimeType)
    }

    @Test
    fun encode_jpeg_keepsHighQuality() = runTest {
        val encoded = renderer().encode(preview(twoColors()), ImageFormat.JPEG)

        val decoded = BitmapFactory.decodeFile(encoded.file.path)
        assertColor(Color.RED, decoded.getPixel(5, 10))
    }

    @Test
    fun encode_png_writesPng() = runTest {
        val encoded = renderer().encode(preview(twoColors()), ImageFormat.PNG)

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(encoded.file.path, options)
        assertEquals("image/png", options.outMimeType)
    }

    @Test
    fun encode_png_isLossless() = runTest {
        val encoded = renderer().encode(preview(twoColors()), ImageFormat.PNG)

        val decoded = BitmapFactory.decodeFile(encoded.file.path)
        assertEquals(Color.BLUE, decoded.getPixel(35, 10))
    }

    @Test
    fun encode_png_keepsTransparency() = runTest {
        val transparent = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

        val encoded = renderer().encode(preview(transparent), ImageFormat.PNG)

        assertEquals(0, Color.alpha(BitmapFactory.decodeFile(encoded.file.path).getPixel(1, 1)))
    }

    @Test(expected = IOException::class)
    fun encode_cacheDirectoryUnusable_throwsIoException() = runTest {
        val blocked = File(context.cacheDir, "editor")
        blocked.deleteRecursively()
        blocked.writeText("a file where the directory should be")
        try {
            renderer().encode(preview(twoColors()), ImageFormat.JPEG)
        } finally {
            blocked.delete()
        }
    }

    @Test
    fun release_recyclesBitmap() = runTest {
        val bitmap = twoColors()

        renderer().release(preview(bitmap))

        assertTrue(bitmap.isRecycled)
    }

    @Test(expected = RenderOutOfMemoryException::class)
    fun guardMemory_outOfMemory_becomesRefusal() {
        guardMemory { throw OutOfMemoryError() }
    }

    @Test
    fun guardMemory_noError_returnsResult() {
        assertEquals(1, guardMemory { 1 })
    }

    private companion object {
        const val TOLERANCE = 12
    }
}
