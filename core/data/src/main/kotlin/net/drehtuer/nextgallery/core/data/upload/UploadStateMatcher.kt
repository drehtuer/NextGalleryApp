package net.drehtuer.nextgallery.core.data.upload

import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.UploadState

/** One local folder compared with its server folder. [previous] are the records of the last check. */
data class FolderComparison(
    val remoteRoot: String,
    val remoteFolderEtag: String?,
    val previous: Map<MediaId, UploadStateRecord>,
    val checkedAtMillis: Long,
)

/** Compares by name and size (D13); a checksum, where the server has one, only confirms such a match. */
object UploadStateMatcher {
    private val CONFIRMED = UploadState.Uploaded(verifiedByChecksum = true)
    private val UNCONFIRMED = UploadState.Uploaded(verifiedByChecksum = false)

    /**
     * [localDigest] reads the image's content, so it is only called for a name-and-size match with a server checksum
     * that has not been compared yet. It returns null if the image can't be read, which leaves the match unconfirmed.
     */
    suspend fun match(
        localItems: List<MediaItem>,
        remoteFiles: List<RemoteItem>,
        comparison: FolderComparison,
        localDigest: suspend (MediaItem, ChecksumAlgorithm) -> String?,
    ): List<UploadStateRecord> {
        val remoteByName = remoteFiles.filterNot { it.isDirectory }.associateBy { it.name }
        return localItems.map { item ->
            val remote = remoteByName[item.displayName]
            val remotePath = RemoteTargetResolver.remotePathFor(item, comparison.remoteRoot)
            val previous = comparison.previous[item.id]?.takeIf { it.remotePath == remotePath }
            UploadStateRecord(
                mediaId = item.id,
                remotePath = remotePath,
                state = stateOf(item, remote, previous, localDigest),
                localSize = item.sizeBytes,
                localDateModifiedSeconds = item.dateModifiedSeconds,
                remoteEtag = remote?.etag,
                remoteFolderEtag = comparison.remoteFolderEtag,
                checkedAtMillis = comparison.checkedAtMillis,
            )
        }
    }

    private suspend fun stateOf(
        item: MediaItem,
        remote: RemoteItem?,
        previous: UploadStateRecord?,
        localDigest: suspend (MediaItem, ChecksumAlgorithm) -> String?,
    ): UploadState = if (remote == null || remote.size != item.sizeBytes) {
        UploadState.NotUploaded
    } else {
        comparedBefore(item, remote, previous) ?: compareChecksums(item, remote, localDigest)
    }

    private suspend fun compareChecksums(
        item: MediaItem,
        remote: RemoteItem,
        localDigest: suspend (MediaItem, ChecksumAlgorithm) -> String?,
    ): UploadState {
        val expected = ChecksumParser.parse(remote.checksums).firstOrNull()
        val actual = expected?.let { localDigest(item, it.algorithm) }
        return when {
            expected == null || actual == null -> UNCONFIRMED
            actual.equals(expected.value, ignoreCase = true) -> CONFIRMED
            else -> UploadState.NotUploaded
        }
    }

    /** The outcome of an earlier comparison of the same local and server file, which is not worth repeating. */
    private fun comparedBefore(item: MediaItem, remote: RemoteItem, previous: UploadStateRecord?): UploadState? {
        val sameFiles = previous != null &&
            remote.etag != null &&
            previous.remoteEtag == remote.etag &&
            previous.localSize == item.sizeBytes &&
            previous.localDateModifiedSeconds == item.dateModifiedSeconds
        return previous?.state?.takeIf { sameFiles && (it == CONFIRMED || it == UploadState.NotUploaded) }
    }
}
