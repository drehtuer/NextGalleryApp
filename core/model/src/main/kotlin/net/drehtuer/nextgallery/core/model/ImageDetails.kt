package net.drehtuer.nextgallery.core.model

/** What the image info sheet shows. EXIF values are null if the image does not have them. */
data class ImageDetails(
    val displayName: String,
    val dateTakenMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val relativePath: String,
    val location: GeoPoint?,
    val camera: String?,
    val exposureTime: String?,
    val aperture: String?,
    val iso: String?,
    val focalLength: String?,
    val place: Place? = null,
)
