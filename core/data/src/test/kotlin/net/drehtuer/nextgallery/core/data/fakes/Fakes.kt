package net.drehtuer.nextgallery.core.data.fakes

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.data.index.ExifSource
import net.drehtuer.nextgallery.core.data.index.GeoIndexStore
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.MediaChangeSource
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.index.MediaScanSource
import net.drehtuer.nextgallery.core.data.index.SyncState
import net.drehtuer.nextgallery.core.data.index.SyncStateStore
import net.drehtuer.nextgallery.core.data.index.TimelineStore
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.DayCount
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.RawExif
import net.drehtuer.nextgallery.core.model.ScannedImage
import java.time.LocalDate
import java.time.ZoneId

class FakeMediaScanSource(
    var images: List<ScannedImage> = emptyList(),
    var version: String = "v1",
    var generation: Long = 10,
) : MediaScanSource {
    var fullScans = 0
        private set
    var changesSinceGeneration: Long? = null
        private set
    var idQueries = 0
        private set
    var queriedFolders: List<String>? = null
        private set

    override suspend fun volumes(): Set<String> = images.map { it.volumeName }.toSet().ifEmpty { setOf(VOLUME) }

    override suspend fun mediaStoreVersion(volume: String): String = version

    override suspend fun currentGeneration(volume: String): Long = generation

    override fun fullScan(volume: String, folders: List<String>): Flow<List<ScannedImage>> {
        fullScans++
        queriedFolders = folders
        return flowOf(images.filter { it.isIn(volume, folders) })
    }

    override fun changesSince(volume: String, generation: Long, folders: List<String>): Flow<List<ScannedImage>> {
        changesSinceGeneration = generation
        queriedFolders = folders
        return flowOf(images.filter { it.isIn(volume, folders) && it.generationModified > generation })
    }

    override suspend fun allIds(volume: String, folders: List<String>): Set<Long> {
        idQueries++
        return images.filter { it.isIn(volume, folders) }.map { it.id.value }.toSet()
    }

    /** What the selection of the real scanner does. */
    private fun ScannedImage.isIn(volume: String, folders: List<String>) =
        volumeName == volume && folders.any { RelativePaths.isUnder(relativePath, it) }

    companion object {
        const val VOLUME = "external_primary"
    }
}

class FakeMediaIndexStore : MediaIndexStore {
    val items = MutableStateFlow<Map<Long, MediaItem>>(emptyMap())
    val folders = MutableStateFlow<List<Folder>>(emptyList())
    var pagingConfig: PagingConfig? = null
        private set

    override suspend fun upsertAll(items: List<MediaItem>) {
        this.items.value += items.associateBy { it.id.value }
    }

    override suspend fun deleteByIds(ids: Collection<Long>) {
        items.value -= ids.toSet()
    }

    override suspend fun idsForVolume(volume: String): Set<Long> =
        items.value.values.filter { it.volumeName == volume }.map { it.id.value }.toSet()

    override suspend fun getById(id: MediaId): MediaItem? = items.value[id.value]

    override suspend fun getByIds(ids: Collection<MediaId>): List<MediaItem> = ids.mapNotNull { items.value[it.value] }

    override fun pagedByBuckets(bucketIds: List<Long>, config: PagingConfig): Flow<PagingData<MediaItem>> {
        pagingConfig = config
        return flowOf(PagingData.from(items.value.values.filter { it.bucketId in bucketIds }))
    }

    override suspend fun idsInBuckets(bucketIds: List<Long>): List<MediaId> =
        items.value.values.filter { it.bucketId in bucketIds }.map { it.id }

    override suspend fun indexInBuckets(bucketIds: List<Long>, id: MediaId): Int =
        items.value.values.filter { it.bucketId in bucketIds }.indexOfFirst { it.id == id }

    override fun observeFolders(): Flow<List<Folder>> = folders
}

class FakeTimelineStore : TimelineStore {
    var items: List<MediaItem> = emptyList()
    val dayCounts = MutableStateFlow<List<DayCount>>(emptyList())

    override fun paged(config: PagingConfig): Flow<PagingData<MediaItem>> = flowOf(PagingData.from(items))

    override suspend fun ids(): List<MediaId> = items.map { it.id }

    override suspend fun indexOf(id: MediaId): Int = items.indexOfFirst { it.id == id }

    override fun observeDayCounts(): Flow<List<DayCount>> = dayCounts
}

class FakeGeoIndexStore : GeoIndexStore {
    val geotagged = MutableStateFlow<List<GeotaggedImage>>(emptyList())
    val unscannedItems = mutableListOf<MediaItem>()
    val saved = mutableMapOf<MediaId, GeoPoint?>()
    val pagedBounds = mutableListOf<GeoBounds>()

    override fun observeGeotagged(): Flow<List<GeotaggedImage>> = geotagged

    override suspend fun unscanned(limit: Int): List<MediaItem> = unscannedItems.take(limit)

    override suspend fun saveLocations(locations: Map<MediaId, GeoPoint?>) {
        saved += locations
        unscannedItems.removeAll { it.id in locations }
    }

    override fun pagedInBounds(bounds: GeoBounds, config: PagingConfig): Flow<PagingData<MediaItem>> {
        pagedBounds += bounds
        return flowOf(PagingData.empty())
    }

    override suspend fun indexInBounds(bounds: GeoBounds, id: MediaId): Int =
        geotagged.value.filter { bounds.contains(it.location) }.indexOfFirst { it.id == id }
}

class FakeSyncStateStore(vararg initial: SyncState) : SyncStateStore {
    val states = initial.associateBy { it.volumeName }.toMutableMap()

    override suspend fun get(volume: String): SyncState? = states[volume]

    override suspend fun put(state: SyncState) {
        states[state.volumeName] = state
    }

    override suspend fun hasAny(): Boolean = states.isNotEmpty()
}

class FakeExifSource(private val exif: RawExif? = null) : ExifSource {
    val readUris = mutableListOf<String>()

    override suspend fun read(contentUri: String): RawExif? {
        readUris += contentUri
        return exif
    }
}

class FakeMediaChangeSource : MediaChangeSource {
    val events = MutableSharedFlow<Unit>()

    override fun changes(): Flow<Unit> = events
}

class FakeWorkScheduler : WorkScheduler {
    val enqueued = mutableListOf<IndexMode>()
    var uploadStateSchedules = 0
        private set
    var uploadStateRequests = 0
        private set

    override fun enqueueIndex(mode: IndexMode) {
        enqueued += mode
    }

    override fun scheduleUploadStateSync() {
        uploadStateSchedules++
    }

    override fun requestUploadStateSync() {
        uploadStateRequests++
    }
}

class FakeClock(var now: Long = 1_000L, var zoneId: ZoneId = ZoneId.of("UTC")) : Clock {
    override fun nowMillis(): Long = now

    override fun zone(): ZoneId = zoneId
}

class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
}

fun mediaItem(id: Long, bucketId: Long = 7, dateTakenMillis: Long = 1, mimeType: String = "image/jpeg") = MediaItem(
    id = MediaId(id),
    volumeName = FakeMediaScanSource.VOLUME,
    contentUri = "content://media/$id",
    bucketId = bucketId,
    bucketName = "Camera",
    relativePath = "DCIM/Camera/",
    displayName = "IMG_$id.jpg",
    mimeType = mimeType,
    sizeBytes = 1,
    width = 1,
    height = 1,
    orientation = 0,
    dateTakenMillis = dateTakenMillis,
    dateSource = DateSource.DATE_TAKEN,
    takenLocalDate = LocalDate.of(2024, 3, 15),
    dateModifiedSeconds = 1,
    generationModified = 1,
    location = null,
)

fun scannedImage(
    id: Long,
    relativePath: String = "DCIM/Camera/",
    dateTakenMillis: Long? = 1_700_000_000_000L,
    generationModified: Long = 1,
    volumeName: String = FakeMediaScanSource.VOLUME,
) = ScannedImage(
    id = MediaId(id),
    volumeName = volumeName,
    contentUri = "content://media/$volumeName/images/media/$id",
    bucketId = relativePath.hashCode().toLong(),
    bucketName = relativePath.trimEnd('/').substringAfterLast('/'),
    relativePath = relativePath,
    displayName = "IMG_$id.jpg",
    mimeType = "image/jpeg",
    sizeBytes = 1_000,
    width = 4_000,
    height = 3_000,
    orientation = 0,
    dateTakenMillis = dateTakenMillis,
    dateModifiedSeconds = 1_700_000_000L,
    generationModified = generationModified,
)
