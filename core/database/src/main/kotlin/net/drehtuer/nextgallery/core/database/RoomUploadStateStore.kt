package net.drehtuer.nextgallery.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import net.drehtuer.nextgallery.core.data.upload.UploadStateRecord
import net.drehtuer.nextgallery.core.data.upload.UploadStateStore
import net.drehtuer.nextgallery.core.database.RoomMediaIndexStore.Companion.SQLITE_MAX_VARIABLES
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.UploadState
import javax.inject.Inject

class RoomUploadStateStore @Inject constructor(private val dao: UploadStateDao) : UploadStateStore {
    override fun observeStates(ids: Collection<MediaId>): Flow<Map<MediaId, UploadState>> {
        if (ids.isEmpty()) return flowOf(emptyMap())
        val chunks = ids.map { it.value }.chunked(SQLITE_MAX_VARIABLES).map { dao.observeStatusFor(it) }
        return combine(chunks) { rows -> rows.flatMap { it }.toStateMap() }
    }

    override suspend fun states(ids: Collection<MediaId>): Map<MediaId, UploadState> = ids.map { it.value }
        .chunked(SQLITE_MAX_VARIABLES)
        .flatMap { dao.statusFor(it) }
        .toStateMap()

    override suspend fun localFolders(): List<String> = dao.localFolders()

    override suspend fun itemsInFolder(relativePath: String): List<MediaItem> =
        dao.mediaInFolder(relativePath).map { it.toModel() }

    override suspend fun recordsInFolder(relativePath: String): List<UploadStateRecord> =
        dao.rowsInFolder(relativePath).map { it.toRecord() }

    override suspend fun upsertAll(records: List<UploadStateRecord>) {
        records.map { it.toEntity() }.chunked(SQLITE_MAX_VARIABLES).forEach { dao.upsertExisting(it) }
    }

    override suspend fun clear() = dao.clear()

    private fun List<UploadStatusRow>.toStateMap(): Map<MediaId, UploadState> =
        associate { MediaId(it.mediaId) to uploadState(it.state, it.checksumVerified) }
}

private fun uploadState(state: Int, checksumVerified: Boolean): UploadState = when (state) {
    UploadStateEntity.STATE_NOT_UPLOADED -> UploadState.NotUploaded
    UploadStateEntity.STATE_UPLOADED -> UploadState.Uploaded(checksumVerified)
    else -> UploadState.Unknown
}

internal fun UploadStateEntity.toRecord() = UploadStateRecord(
    mediaId = MediaId(mediaId),
    remotePath = remotePath,
    state = uploadState(state, checksumVerified),
    localSize = localSize,
    localDateModifiedSeconds = localDateModifiedSeconds,
    remoteEtag = remoteEtag,
    remoteFolderEtag = remoteFolderEtag,
    checkedAtMillis = checkedAtMillis,
)

internal fun UploadStateRecord.toEntity() = UploadStateEntity(
    mediaId = mediaId.value,
    remotePath = remotePath,
    state = when (state) {
        UploadState.Unknown -> UploadStateEntity.STATE_UNKNOWN
        UploadState.NotUploaded -> UploadStateEntity.STATE_NOT_UPLOADED
        is UploadState.Uploaded -> UploadStateEntity.STATE_UPLOADED
    },
    checksumVerified = (state as? UploadState.Uploaded)?.verifiedByChecksum == true,
    localSize = localSize,
    localDateModifiedSeconds = localDateModifiedSeconds,
    remoteEtag = remoteEtag,
    remoteFolderEtag = remoteFolderEtag,
    checkedAtMillis = checkedAtMillis,
)
