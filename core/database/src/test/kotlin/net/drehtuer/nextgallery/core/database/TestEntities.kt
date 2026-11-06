package net.drehtuer.nextgallery.core.database

import net.drehtuer.nextgallery.core.model.DateSource

fun mediaEntity(id: Long, volumeName: String = "external_primary") = MediaEntity(
    id = id,
    volumeName = volumeName,
    contentUri = "content://media/$volumeName/images/media/$id",
    bucketId = 7,
    bucketName = "Camera",
    relativePath = "DCIM/Camera/",
    displayName = "IMG_$id.jpg",
    mimeType = "image/jpeg",
    sizeBytes = 1_000,
    width = 4_000,
    height = 3_000,
    orientation = 90,
    dateTakenMillis = 1_710_500_000_000L,
    dateSource = DateSource.DATE_TAKEN,
    dateModifiedSeconds = 1_710_500_000L,
    generationModified = 3,
    localYear = 2024,
    localMonth = 3,
    localDay = 15,
    latitude = 48.1,
    longitude = 11.5,
    exifScanned = false,
)
