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
import net.drehtuer.nextgallery.core.data.index.ExifSource
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.MediaChangeSource
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.index.MediaScanSource
import net.drehtuer.nextgallery.core.data.index.SyncState
import net.drehtuer.nextgallery.core.data.index.SyncStateStore
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.RawExif
import net.drehtuer.nextgallery.core.model.ScannedImage
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

    override suspend fun volumes(): Set<String> = images.map { it.volumeName }.toSet().ifEmpty { setOf(VOLUME) }

    override suspend fun mediaStoreVersion(volume: String): String = version

    override suspend fun currentGeneration(volume: String): Long = generation

    override fun fullScan(volume: String): Flow<List<ScannedImage>> {
        fullScans++
        return flowOf(images.filter { it.volumeName == volume })
    }

    override fun changesSince(volume: String, generation: Long): Flow<List<ScannedImage>> {
        changesSinceGeneration = generation
        return flowOf(images.filter { it.volumeName == volume && it.generationModified > generation })
    }

    override suspend fun allIds(volume: String): Set<Long> =
        images.filter { it.volumeName == volume }.map { it.id.value }.toSet()

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

    override fun pagedByBucket(bucketId: Long, config: PagingConfig): Flow<PagingData<MediaItem>> {
        pagingConfig = config
        return flowOf(PagingData.from(items.value.values.filter { it.bucketId == bucketId }))
    }

    override suspend fun idsInBucket(bucketId: Long): List<MediaId> =
        items.value.values.filter { it.bucketId == bucketId }.map { it.id }

    override fun observeFolders(): Flow<List<Folder>> = folders
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

    override fun enqueueIndex(mode: IndexMode) {
        enqueued += mode
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
