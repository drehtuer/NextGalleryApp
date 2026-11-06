package net.drehtuer.nextgallery.core.model

import java.time.LocalDate

/** An indexed local image. */
data class MediaItem(
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
    val dateTakenMillis: Long,
    val dateSource: DateSource,
    val takenLocalDate: LocalDate,
    val dateModifiedSeconds: Long,
    val generationModified: Long,
    val location: GeoPoint?,
)
