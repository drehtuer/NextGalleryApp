package net.drehtuer.nextgallery.core.data.media

import net.drehtuer.nextgallery.core.data.index.GpsParser
import net.drehtuer.nextgallery.core.model.ImageDetails
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.RawExif
import java.util.Locale
import kotlin.math.roundToInt

/** Turns raw EXIF values into the text the image info sheet shows. */
object ImageDetailsMapper {
    fun from(exif: RawExif?, item: MediaItem) = ImageDetails(
        displayName = item.displayName,
        dateTakenMillis = item.dateTakenMillis,
        sizeBytes = item.sizeBytes,
        width = item.width,
        height = item.height,
        mimeType = item.mimeType,
        relativePath = item.relativePath,
        // The index only has the location after the second pass; the file may already be open here.
        location = item.location ?: exif?.let(GpsParser::parse),
        camera = camera(exif?.make, exif?.model),
        exposureTime = exposureTime(exif?.exposureTime),
        aperture = number(exif?.fNumber)?.let { "f/$it" },
        iso = exif?.iso?.takeIf { it.isNotBlank() },
        focalLength = number(exif?.focalLength)?.let { "$it mm" },
    )

    /** Many cameras repeat the make in the model, e.g. "Canon" and "Canon EOS R6". */
    private fun camera(make: String?, model: String?): String? {
        val cleanMake = make?.trim().orEmpty()
        val cleanModel = model?.trim().orEmpty()
        return when {
            cleanModel.startsWith(cleanMake, ignoreCase = true) -> cleanModel
            cleanModel.isEmpty() -> cleanMake
            else -> "$cleanMake $cleanModel"
        }.ifEmpty { null }
    }

    private fun exposureTime(raw: String?): String? {
        val seconds = parse(raw)?.takeIf { it > 0 } ?: return null
        return if (seconds >= 1) "${format(seconds)} s" else "1/${(1 / seconds).roundToInt()} s"
    }

    private fun number(raw: String?): String? = parse(raw)?.takeIf { it > 0 }?.let(::format)

    /** EXIF stores these either as a decimal or as a fraction such as "27/5". */
    private fun parse(raw: String?): Double? {
        val parts = raw?.split('/').orEmpty()
        val value = parts.firstOrNull()?.trim()?.toDoubleOrNull()
        val divisor = if (parts.size > 1) parts[1].trim().toDoubleOrNull() else 1.0
        return divisor?.takeIf { it != 0.0 }?.let { value?.div(it) }
    }

    private fun format(value: Double): String = String.format(Locale.ROOT, "%.1f", value).removeSuffix(".0")
}
