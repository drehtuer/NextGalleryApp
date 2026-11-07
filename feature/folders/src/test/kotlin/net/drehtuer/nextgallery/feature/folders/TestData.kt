package net.drehtuer.nextgallery.feature.folders

import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.VirtualFolder
import net.drehtuer.nextgallery.core.model.VirtualFolderSummary
import java.time.LocalDate

fun mediaItem(id: Long, bucketId: Long = 7) = MediaItem(
    id = MediaId(id),
    volumeName = "external_primary",
    contentUri = "content://media/$id",
    bucketId = bucketId,
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
    takenLocalDate = LocalDate.of(2024, 3, 15),
    dateModifiedSeconds = 1,
    generationModified = 1,
    location = null,
)

fun folder(bucketId: Long = 7, name: String = "Camera", itemCount: Int = 1) = Folder(
    bucketId = bucketId,
    name = name,
    relativePath = "DCIM/$name/",
    itemCount = itemCount,
    coverItem = mediaItem(1, bucketId),
)

fun virtualFolder(id: String = "v1", name: String = "Everything", itemCount: Int = 2) = VirtualFolderSummary(
    folder = VirtualFolder(id, name, listOf(7, 8)),
    itemCount = itemCount,
    coverItem = mediaItem(1),
)
