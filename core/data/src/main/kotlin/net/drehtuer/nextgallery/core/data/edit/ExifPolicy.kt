package net.drehtuer.nextgallery.core.data.edit

/**
 * Which EXIF tags an edited image keeps (D12). The pixels are upright and resized, so the orientation is reset and
 * the dimensions are new; the embedded thumbnail would show the old picture, so it is not among the copied tags.
 */
object ExifPolicy {
    private const val ORIENTATION = "Orientation"
    private const val ORIENTATION_NORMAL = "1"
    private const val IMAGE_WIDTH = "ImageWidth"
    private const val IMAGE_LENGTH = "ImageLength"
    private const val PIXEL_X_DIMENSION = "PixelXDimension"
    private const val PIXEL_Y_DIMENSION = "PixelYDimension"

    val COPIED_TAGS: Set<String> = setOf(
        "Artist",
        "Copyright",
        "ImageDescription",
        "UserComment",
        "Make",
        "Model",
        "Software",
        "LensMake",
        "LensModel",
        "DateTime",
        "DateTimeOriginal",
        "DateTimeDigitized",
        "OffsetTime",
        "OffsetTimeOriginal",
        "OffsetTimeDigitized",
        "SubSecTime",
        "SubSecTimeOriginal",
        "SubSecTimeDigitized",
        "ExposureTime",
        "ExposureProgram",
        "ExposureBiasValue",
        "ExposureMode",
        "FNumber",
        "ApertureValue",
        "MaxApertureValue",
        "ShutterSpeedValue",
        "BrightnessValue",
        "PhotographicSensitivity",
        "FocalLength",
        "FocalLengthIn35mmFilm",
        "Flash",
        "WhiteBalance",
        "MeteringMode",
        "SceneCaptureType",
        "DigitalZoomRatio",
        "GPSLatitude",
        "GPSLatitudeRef",
        "GPSLongitude",
        "GPSLongitudeRef",
        "GPSAltitude",
        "GPSAltitudeRef",
        "GPSTimeStamp",
        "GPSDateStamp",
        "GPSProcessingMethod",
        "GPSImgDirection",
        "GPSImgDirectionRef",
        "GPSSpeed",
        "GPSSpeedRef",
    )

    fun forOutput(sourceTags: Map<String, String>, newWidth: Int, newHeight: Int): Map<String, String> =
        sourceTags.filterKeys { it in COPIED_TAGS } + mapOf(
            ORIENTATION to ORIENTATION_NORMAL,
            IMAGE_WIDTH to newWidth.toString(),
            IMAGE_LENGTH to newHeight.toString(),
            PIXEL_X_DIMENSION to newWidth.toString(),
            PIXEL_Y_DIMENSION to newHeight.toString(),
        )
}
