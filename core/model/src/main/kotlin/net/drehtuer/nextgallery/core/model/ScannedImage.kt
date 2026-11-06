package net.drehtuer.nextgallery.core.model

/** One image row as read from MediaStore, before dates are resolved. */
data class ScannedImage(
    val id: MediaId,
    val volumeName: String,
    val contentUri: String,
    val bucketId: Long,
    val bucketName: String,
    val relativePath: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val dateTakenMillis: Long?,
    val dateModifiedSeconds: Long,
    val generationModified: Long,
)
