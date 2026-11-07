package net.drehtuer.nextgallery.core.data.upload

import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.UploadState
import java.io.InputStream

/** What the last check found for an image, with what is needed to tell whether it is still valid. */
data class UploadStateRecord(
    val mediaId: MediaId,
    val remotePath: String,
    val state: UploadState,
    val localSize: Long,
    val localDateModifiedSeconds: Long,
    /** The file on the server; null if there is none. */
    val remoteEtag: String?,
    /** The server folder at the time of the check; it changes with anything in that folder. */
    val remoteFolderEtag: String?,
    val checkedAtMillis: Long,
)

/** Port: the `upload_state` table. Implemented with Room in `:core:database`. */
interface UploadStateStore {
    /**
     * The states of those of [ids] that have a record and did not change locally since it was written. Never waits for
     * the network, and is independent of the paging queries.
     */
    fun observeStates(ids: Collection<MediaId>): Flow<Map<MediaId, UploadState>>

    /** One reading of [observeStates]. */
    suspend fun states(ids: Collection<MediaId>): Map<MediaId, UploadState>

    /** The relative paths of all indexed folders. */
    suspend fun localFolders(): List<String>

    suspend fun itemsInFolder(relativePath: String): List<MediaItem>

    suspend fun recordsInFolder(relativePath: String): List<UploadStateRecord>

    /** Records of images that are no longer in the index are left out. */
    suspend fun upsertAll(records: List<UploadStateRecord>)

    suspend fun clear()
}

/** Port: the original bytes of a local image, for checksums. */
fun interface MediaContentSource {
    /** Null if the system has no such content. Throws if it can't be read. */
    fun open(contentUri: String): InputStream?
}

/** Port: whether the Nextcloud app is on this device. */
fun interface NextcloudAppSource {
    fun isInstalled(): Boolean
}
