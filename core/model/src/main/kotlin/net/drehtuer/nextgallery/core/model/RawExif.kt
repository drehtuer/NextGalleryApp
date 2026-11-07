package net.drehtuer.nextgallery.core.model

/** EXIF values exactly as stored in the file; parsing happens in `:core:data`. */
data class RawExif(
    val dateTimeOriginal: String?,
    val offsetTimeOriginal: String?,
    val subSecTimeOriginal: String?,
    val make: String? = null,
    val model: String? = null,
    val exposureTime: String? = null,
    val fNumber: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val gpsLatitude: String? = null,
    val gpsLatitudeRef: String? = null,
    val gpsLongitude: String? = null,
    val gpsLongitudeRef: String? = null,
)
