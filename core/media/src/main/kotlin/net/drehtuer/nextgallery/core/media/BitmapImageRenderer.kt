package net.drehtuer.nextgallery.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.common.deleteIfPossible
import net.drehtuer.nextgallery.core.data.edit.EncodedImage
import net.drehtuer.nextgallery.core.data.edit.ImageFormat
import net.drehtuer.nextgallery.core.data.edit.ImageRenderer
import net.drehtuer.nextgallery.core.data.edit.RenderOutOfMemoryException
import net.drehtuer.nextgallery.core.data.edit.RenderPlan
import net.drehtuer.nextgallery.core.data.edit.RenderStroke
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import java.io.IOException
import javax.inject.Inject

/**
 * Applies a [RenderPlan] with `Matrix`, `Canvas` and `Path`. `ImageDecoder` delivers upright pixels (it applies the
 * EXIF orientation) in software bitmaps. Colour space and bit depth of the source are kept.
 */
class BitmapImageRenderer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : ImageRenderer {
    override suspend fun readSize(contentUri: String): ImageSize = withContext(dispatchers.io) {
        var size: ImageSize? = null
        try {
            ImageDecoder.decodeBitmap(source(contentUri)) { _, info, _ ->
                size = ImageSize(info.size.width, info.size.height)
                // Only the header is needed; ImageDecoder has no other way to stop before decoding the pixels.
                throw HeaderRead()
            }
        } catch (_: HeaderRead) {
            // Expected, see above.
        }
        size ?: throw IOException("Cannot read the size of the image")
    }

    override suspend fun decode(contentUri: String, sampleSize: Int): PreviewImage = withContext(dispatchers.io) {
        guardMemory {
            val bitmap = ImageDecoder.decodeBitmap(source(contentUri)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(sampleSize)
            }
            bitmap.toPreviewImage()
        }
    }

    override suspend fun render(base: PreviewImage, plan: RenderPlan): PreviewImage = guardMemory {
        val source = base.handle as Bitmap
        val output = createBitmap(
            plan.outputSize.width,
            plan.outputSize.height,
            source.config ?: Bitmap.Config.ARGB_8888,
            true,
            source.colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB),
        )
        val canvas = Canvas(output)
        canvas.drawBitmap(source, plan.matrixFor(source), Paint(Paint.FILTER_BITMAP_FLAG))
        plan.strokes.forEach { canvas.drawStroke(it) }
        output.toPreviewImage()
    }

    override suspend fun encode(image: PreviewImage, format: ImageFormat): EncodedImage = withContext(dispatchers.io) {
        val file = EditorFiles.newTemporaryFile(context, "encoded")
        val written = try {
            file.outputStream().use { stream ->
                when (format) {
                    ImageFormat.JPEG -> (image.handle as Bitmap).compress(
                        Bitmap.CompressFormat.JPEG,
                        JPEG_QUALITY,
                        stream,
                    )

                    ImageFormat.PNG -> (image.handle as Bitmap).compress(
                        Bitmap.CompressFormat.PNG,
                        PNG_QUALITY,
                        stream,
                    )
                }
            }
        } catch (e: IOException) {
            file.deleteIfPossible()
            throw e
        }
        if (!written) {
            file.deleteIfPossible()
            throw IOException("Cannot encode the image")
        }
        EncodedImage(file)
    }

    override fun release(image: PreviewImage) {
        (image.handle as Bitmap).recycle()
    }

    private fun source(contentUri: String) = ImageDecoder.createSource(context.contentResolver, contentUri.toUri())

    private fun Bitmap.toPreviewImage() = PreviewImage(this, width, height)

    /** The plan maps full-size pixels; [source] can be a downsampled version of them. */
    private fun RenderPlan.matrixFor(source: Bitmap): Matrix = Matrix().apply {
        setValues(
            floatArrayOf(
                sourceMatrix.scaleX.toFloat(),
                sourceMatrix.skewX.toFloat(),
                sourceMatrix.translateX.toFloat(),
                sourceMatrix.skewY.toFloat(),
                sourceMatrix.scaleY.toFloat(),
                sourceMatrix.translateY.toFloat(),
                0f,
                0f,
                1f,
            ),
        )
        preScale(sourceSize.width.toFloat() / source.width, sourceSize.height.toFloat() / source.height)
    }

    private fun Canvas.drawStroke(stroke: RenderStroke) {
        val first = stroke.points.firstOrNull() ?: return
        val path = Path().apply {
            moveTo(first.x.toFloat(), first.y.toFloat())
            // A single point has to become a line as well, for the round caps to draw a dot.
            lineTo(first.x.toFloat(), first.y.toFloat())
            stroke.points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.widthPixels.toFloat()
            color = stroke.colorArgb
        }
        drawPath(path, paint)
    }

    private class HeaderRead : RuntimeException()

    private companion object {
        const val JPEG_QUALITY = 95

        /** PNG is lossless; the value only exists because the function needs one. */
        const val PNG_QUALITY = 100
    }
}

/** The memory check before (D30) is an estimate; if the platform still runs out, this is a refusal and no crash. */
internal inline fun <T> guardMemory(block: () -> T): T = try {
    block()
} catch (e: OutOfMemoryError) {
    throw RenderOutOfMemoryException(e)
}
