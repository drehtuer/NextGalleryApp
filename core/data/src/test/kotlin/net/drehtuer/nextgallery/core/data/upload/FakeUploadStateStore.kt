package net.drehtuer.nextgallery.core.data.upload

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.UploadState

internal class FakeUploadStateStore(items: List<MediaItem> = emptyList()) : UploadStateStore {
    var items = items
    val records = MutableStateFlow<Map<MediaId, UploadStateRecord>>(emptyMap())
    val states = MutableStateFlow<Map<MediaId, UploadState>>(emptyMap())
    var upserts = 0
        private set

    override fun observeStates(ids: Collection<MediaId>): Flow<Map<MediaId, UploadState>> =
        states.map { all -> all.filterKeys { it in ids } }

    override suspend fun states(ids: Collection<MediaId>): Map<MediaId, UploadState> =
        states.value.filterKeys { it in ids }

    override suspend fun localFolders(): List<String> = items.map { it.relativePath }.distinct()

    override suspend fun itemsInFolder(relativePath: String): List<MediaItem> =
        items.filter { it.relativePath == relativePath }

    override suspend fun recordsInFolder(relativePath: String): List<UploadStateRecord> {
        val ids = itemsInFolder(relativePath).map { it.id }
        return records.value.values.filter { it.mediaId in ids }
    }

    override suspend fun upsertAll(records: List<UploadStateRecord>) {
        upserts++
        this.records.value += records.associateBy { it.mediaId }
    }

    override suspend fun clear() {
        records.value = emptyMap()
    }
}
