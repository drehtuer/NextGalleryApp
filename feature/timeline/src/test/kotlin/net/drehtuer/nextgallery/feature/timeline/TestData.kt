package net.drehtuer.nextgallery.feature.timeline

import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import java.time.LocalDate

fun mediaItem(id: Long) = MediaItem(
    id = MediaId(id),
    volumeName = "external_primary",
    contentUri = "content://media/$id",
    bucketId = 7,
    bucketName = "Camera",
    relativePath = "DCIM/Camera/",
    displayName = "IMG_$id.jpg",
    mimeType = "image/jpeg",
    sizeBytes = 1,
    width = 1,
    height = 1,
    orientation = 0,
    dateTakenMillis = 1,
    dateSource = DateSource.DATE_TAKEN,
    takenLocalDate = LocalDate.of(2024, 3, 16),
    dateModifiedSeconds = 1,
    generationModified = 1,
    location = null,
)
