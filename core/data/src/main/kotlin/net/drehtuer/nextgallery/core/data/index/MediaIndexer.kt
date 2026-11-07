package net.drehtuer.nextgallery.core.data.index

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.data.rules.FolderRuleMatcher
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.ScannedImage
import javax.inject.Inject

/**
 * Keeps the local index in sync with MediaStore (ARCHITECTURE.md §5.1): a full scan on first run or when the
 * MediaStore version changed, otherwise only rows with a newer `GENERATION_MODIFIED`. Deletions are found by
 * comparing ID sets, because generation queries do not report them.
 */
class MediaIndexer @Inject constructor(
    private val scanSource: MediaScanSource,
    private val exifSource: ExifSource,
    private val indexStore: MediaIndexStore,
    private val syncStateStore: SyncStateStore,
    private val settingsRepository: SettingsRepository,
    private val exifDateParser: ExifDateParser,
    private val dateResolver: DateResolver,
    private val clock: Clock,
) {
    suspend fun sync(mode: IndexMode): IndexStats {
        val settings = settingsRepository.observeSettings().first()
        val matcher = FolderRuleMatcher(settings.folderRules, settings.excludePatterns)
        return scanSource.volumes().fold(IndexStats.EMPTY) { stats, volume ->
            stats + syncVolume(volume, mode, matcher)
        }
    }

    private suspend fun syncVolume(volume: String, mode: IndexMode, matcher: FolderRuleMatcher): IndexStats {
        val previous = syncStateStore.get(volume)
        val version = scanSource.mediaStoreVersion(volume)
        // Read before scanning, so changes made during the scan are picked up by the next sync.
        val generation = scanSource.currentGeneration(volume)
        val full = mode == IndexMode.FULL || previous == null || previous.mediaStoreVersion != version

        val batches = if (full) {
            scanSource.fullScan(volume)
        } else {
            scanSource.changesSince(volume, previous.lastGeneration)
        }
        val seen = HashSet<Long>()
        val excluded = HashSet<Long>()
        var upserted = 0
        batches.collect { batch ->
            currentCoroutineContext().ensureActive()
            val (indexed, skipped) = batch.partition { matcher.isIndexed(it.relativePath) }
            val known = indexStore.getByIds(indexed.map { it.id }).associateBy { it.id }
            indexStore.upsertAll(indexed.map { it.toMediaItem(known[it.id]) })
            indexed.mapTo(seen) { it.id.value }
            skipped.mapTo(excluded) { it.id.value }
            upserted += indexed.size
        }

        val stored = indexStore.idsForVolume(volume)
        val toDelete = if (full) stored - seen else (stored - scanSource.allIds(volume)) + (excluded intersect stored)
        if (toDelete.isNotEmpty()) indexStore.deleteByIds(toDelete)

        val lastFullScan = if (full) clock.nowMillis() else previous.lastFullScanMillis
        syncStateStore.put(SyncState(volume, generation, version, lastFullScan))
        return IndexStats(upserted = upserted, deleted = toDelete.size)
    }

    /** The location comes from [LocationBackfill]. It is only looked for again if the file changed since [known]. */
    private suspend fun ScannedImage.toMediaItem(known: MediaItem?): MediaItem {
        val scanned = known?.takeIf {
            it.exifScanned && it.sizeBytes == sizeBytes && it.dateModifiedSeconds == dateModifiedSeconds
        }
        val dateTaken = dateTakenMillis
        val exif = if (dateTaken == null || dateTaken <= 0) {
            exifSource.read(contentUri)?.let(exifDateParser::parse)
        } else {
            null
        }
        val date = dateResolver.resolve(dateTakenMillis, exif, dateModifiedSeconds, clock.zone())
        return MediaItem(
            id = id,
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
            dateTakenMillis = date.epochMillis,
            dateSource = date.source,
            takenLocalDate = date.localDate,
            dateModifiedSeconds = dateModifiedSeconds,
            generationModified = generationModified,
            location = scanned?.location,
            exifScanned = scanned != null,
        )
    }
}
