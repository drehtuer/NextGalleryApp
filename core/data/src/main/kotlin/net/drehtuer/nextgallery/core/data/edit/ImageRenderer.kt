package net.drehtuer.nextgallery.core.data.edit

import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import java.io.File

/** The formats the editor can write; an edit keeps the format of its source (D29). */
enum class ImageFormat {
    JPEG,
    PNG,
    ;

    companion object {
        fun fromMimeType(mimeType: String): ImageFormat? = when (mimeType.lowercase()) {
            "image/jpeg" -> JPEG
            "image/png" -> PNG
            else -> null
        }
    }
}

/** An encoded image in a temporary file of the app. Whoever receives it deletes the file. */
class EncodedImage(val file: File)

/** The platform ran out of memory in spite of the check before (D30). */
class RenderOutOfMemoryException(cause: Throwable) : RuntimeException(cause)

/**
 * Port: decodes, draws and encodes images. Implemented with `Bitmap` in `:core:media`.
 * Each function throws an `IOException` if it fails and a [RenderOutOfMemoryException] without enough memory.
 */
interface ImageRenderer {
    /** The upright size, with the EXIF orientation applied. */
    suspend fun readSize(contentUri: String): ImageSize

    /** The upright image, reduced by [sampleSize] (a power of two). */
    suspend fun decode(contentUri: String, sampleSize: Int): PreviewImage

    /** Draws a new image; [base] is the source of [plan] at any resolution and stays as it is. */
    suspend fun render(base: PreviewImage, plan: RenderPlan): PreviewImage

    suspend fun encode(image: PreviewImage, format: ImageFormat): EncodedImage

    /** Frees the memory of an image nobody shows. */
    fun release(image: PreviewImage)
}
