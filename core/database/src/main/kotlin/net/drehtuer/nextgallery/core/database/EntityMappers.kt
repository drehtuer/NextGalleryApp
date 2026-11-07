package net.drehtuer.nextgallery.core.database

import net.drehtuer.nextgallery.core.data.index.SyncState
import net.drehtuer.nextgallery.core.model.DayCount
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import java.time.LocalDate

internal fun MediaEntity.toModel() = MediaItem(
    id = MediaId(id),
    volumeName = volumeName,
    contentUri = contentUri,
    bucketId = bucketId,
    bucketName = bucketName,
    relativePath = relativePath,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    orientation = orientation,
    dateTakenMillis = dateTakenMillis,
    dateSource = dateSource,
    takenLocalDate = LocalDate.of(localYear, localMonth, localDay),
    dateModifiedSeconds = dateModifiedSeconds,
    generationModified = generationModified,
    location = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
)

internal fun MediaItem.toEntity() = MediaEntity(
    id = id.value,
    volumeName = volumeName,
    contentUri = contentUri,
    bucketId = bucketId,
    bucketName = bucketName,
    relativePath = relativePath,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    orientation = orientation,
    dateTakenMillis = dateTakenMillis,
    dateSource = dateSource,
    dateModifiedSeconds = dateModifiedSeconds,
    generationModified = generationModified,
    localYear = takenLocalDate.year,
    localMonth = takenLocalDate.monthValue,
    localDay = takenLocalDate.dayOfMonth,
    latitude = location?.latitude,
    longitude = location?.longitude,
    exifScanned = false,
)

internal fun DayCountRow.toModel() = DayCount(LocalDate.of(year, month, day), count)

internal fun FolderRow.toModel() = Folder(
    bucketId = cover.bucketId,
    name = cover.bucketName,
    relativePath = cover.relativePath,
    itemCount = itemCount,
    coverItem = cover.toModel(),
)

internal fun SyncStateEntity.toModel() = SyncState(volumeName, lastGeneration, mediaStoreVersion, lastFullScanMillis)

internal fun SyncState.toEntity() = SyncStateEntity(volumeName, lastGeneration, mediaStoreVersion, lastFullScanMillis)
