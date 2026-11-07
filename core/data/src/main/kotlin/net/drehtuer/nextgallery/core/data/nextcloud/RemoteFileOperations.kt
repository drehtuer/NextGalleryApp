package net.drehtuer.nextgallery.core.data.nextcloud

import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import net.drehtuer.nextgallery.core.nextcloud.TrashItem
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import javax.inject.Inject
import javax.inject.Singleton

/** What to do if the target already has a file of that name (D46). */
enum class ConflictPolicy { ASK, KEEP_BOTH, REPLACE, SKIP }

sealed interface RemoteMoveResult {
    val item: RemoteItem

    data class Moved(override val item: RemoteItem, val newPath: String) : RemoteMoveResult

    /** "Keep both": the moved file got a new name. */
    data class Renamed(override val item: RemoteItem, val newPath: String) : RemoteMoveResult

    data class Replaced(override val item: RemoteItem, val newPath: String) : RemoteMoveResult

    data class Skipped(override val item: RemoteItem) : RemoteMoveResult

    /** Only with [ConflictPolicy.ASK]: nothing happened yet. [existing] is null if it could not be read. */
    data class Conflict(override val item: RemoteItem, val existing: RemoteItem?) : RemoteMoveResult

    data class Failed(override val item: RemoteItem, val error: AppError) : RemoteMoveResult
}

/** Where the image is now, if the move changed anything. */
internal val RemoteMoveResult.newPath: String?
    get() = when (this) {
        is RemoteMoveResult.Moved -> newPath
        is RemoteMoveResult.Renamed -> newPath
        is RemoteMoveResult.Replaced -> newPath
        is RemoteMoveResult.Skipped, is RemoteMoveResult.Conflict, is RemoteMoveResult.Failed -> null
    }

data class RemoteBatchResult(val done: List<RemoteItem>, val failures: List<Pair<RemoteItem, AppError>>)

/** A folder for the move sheet: [folders] are its subfolders, [self] tells whether it can be written to. */
data class RemoteFolderContent(val self: RemoteItem?, val folders: List<RemoteItem>)

interface RemoteFileOperations {
    suspend fun move(
        items: List<RemoteItem>,
        targetFolder: String,
        conflictPolicy: ConflictPolicy = ConflictPolicy.ASK,
    ): List<RemoteMoveResult>

    /**
     * Moves the images back, last moved first, and never over a file that has appeared at the old place in the
     * meantime.
     */
    suspend fun undoMove(results: List<RemoteMoveResult>): RemoteBatchResult

    /** Into the Nextcloud trash bin if it is on ([isTrashBinEnabled]); otherwise the images are gone for good. */
    suspend fun delete(items: List<RemoteItem>): RemoteBatchResult

    /**
     * Whether the server keeps deleted files in its trash bin (D56). Unknown counts as no: logged out, or the answer
     * could not be read. A known answer is kept for the signed-in account until the app ends.
     */
    suspend fun isTrashBinEnabled(): Boolean

    suspend fun undoDelete(items: List<RemoteItem>): RemoteBatchResult

    suspend fun listFolder(path: String): AppResult<RemoteFolderContent>

    /** Returns the path of the new folder. */
    suspend fun createFolder(parent: String, name: String): AppResult<String>

    /** Whether files can be added: `oc:permissions` has C or K (D46). Unknown means yes; the server still decides. */
    fun canWrite(folder: RemoteItem?): Boolean

    /** "IMG_1.jpg" → "IMG_1 (2).jpg", with the first number not in [existing]. */
    fun nextFreeName(name: String, existing: Set<String>): String
}

/** A singleton, so that the trash bin answer lasts for the session. */
@Singleton
class DefaultRemoteFileOperations @Inject constructor(
    private val runner: RemoteCallRunner,
    private val webDav: WebDavClient,
    ocs: OcsClient,
) : RemoteFileOperations {
    private val mover = RemoteMover(runner, webDav, ::nextFreeName)
    private val trashBinCheck = TrashBinCheck(runner, ocs)

    override suspend fun move(
        items: List<RemoteItem>,
        targetFolder: String,
        conflictPolicy: ConflictPolicy,
    ): List<RemoteMoveResult> = items.map { item ->
        mover.move(item, RemotePaths.normalize(targetFolder), conflictPolicy)
    }

    override suspend fun undoMove(results: List<RemoteMoveResult>): RemoteBatchResult = batch(
        // Later moves may have taken the place of earlier ones, so going backwards each path holds the expected image.
        results.asReversed().mapNotNull { result -> result.newPath?.let { result.item to it } },
    ) { account, (item, newPath) -> webDav.move(account, newPath, item.path, overwrite = false) }
        .let { batch ->
            RemoteBatchResult(
                batch.done.map {
                    it.first
                },
                batch.failures.map { (moved, error) -> moved.first to error },
            )
        }

    override suspend fun delete(items: List<RemoteItem>): RemoteBatchResult = batch(items) { account, item ->
        webDav.delete(account, item.path)
    }.let { RemoteBatchResult(it.done, it.failures) }

    override suspend fun isTrashBinEnabled(): Boolean = trashBinCheck.isEnabled()

    override suspend fun undoDelete(items: List<RemoteItem>): RemoteBatchResult {
        val trash = when (val result = runner.call { webDav.listTrash(it) }) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return RemoteBatchResult(emptyList(), items.map { it to result.error })
        }
        val done = mutableListOf<RemoteItem>()
        val failures = mutableListOf<Pair<RemoteItem, AppError>>()
        items.forEach { item ->
            val entry = latestEntryFor(item, trash)
            if (entry == null) {
                failures += item to AppError.NotFound
            } else {
                when (val result = runner.call { webDav.restore(it, entry) }) {
                    is AppResult.Success -> done += item
                    is AppResult.Failure -> failures += item to result.error
                }
            }
        }
        return RemoteBatchResult(done, failures)
    }

    /**
     * An older file of that name may be in the trash as well, so the same file id decides. Only where an id is unknown
     * can the path decide, with the newest entry: an entry with another id is another file, and restoring it would be
     * wrong.
     */
    private fun latestEntryFor(item: RemoteItem, trash: List<TrashItem>): TrashItem? {
        val candidates = trash.filter { it.originalPath == item.path }
        val eligible = if (item.fileId == null) {
            candidates
        } else {
            candidates.filter { it.fileId == item.fileId }.ifEmpty { candidates.filter { it.fileId == null } }
        }
        return eligible.maxByOrNull { it.deletionTimeSeconds }
    }

    override suspend fun listFolder(path: String): AppResult<RemoteFolderContent> =
        when (val result = runner.call { webDav.list(it, path) }) {
            is AppResult.Success -> AppResult.Success(
                RemoteFolderContent(
                    self = result.value.self,
                    folders = result.value.children.filter { it.isDirectory }.sortedBy { it.name.lowercase() },
                ),
            )

            is AppResult.Failure -> result
        }

    override suspend fun createFolder(parent: String, name: String): AppResult<String> {
        val trimmed = name.trim()
        if (!RemotePaths.isValidName(trimmed)) return AppResult.Failure(AppError.InvalidName)
        val path = RemotePaths.join(parent, trimmed)
        return when (val result = runner.call { webDav.createFolder(it, path) }) {
            is AppResult.Success -> AppResult.Success(path)
            is AppResult.Failure -> result
        }
    }

    override fun canWrite(folder: RemoteItem?): Boolean {
        val permissions = folder?.permissions ?: return true
        return WRITE_PERMISSIONS.any { it in permissions }
    }

    override fun nextFreeName(name: String, existing: Set<String>): String {
        val dot = name.lastIndexOf('.').takeIf { it > 0 }
        val base = dot?.let { name.substring(0, it) } ?: name
        val extension = dot?.let { name.substring(it) }.orEmpty()
        return generateSequence(FIRST_SUFFIX) { it + 1 }
            .map { number -> "$base ($number)$extension" }
            .first { it !in existing }
    }

    private suspend fun <T> batch(items: List<T>, action: suspend (NextcloudAccount, T) -> Unit): Batch<T> {
        val done = mutableListOf<T>()
        val failures = mutableListOf<Pair<T, AppError>>()
        items.forEach { item ->
            when (val result = runner.call { action(it, item) }) {
                is AppResult.Success -> done += item
                is AppResult.Failure -> failures += item to result.error
            }
        }
        return Batch(done, failures)
    }

    private data class Batch<T>(val done: List<T>, val failures: List<Pair<T, AppError>>)

    private companion object {
        const val WRITE_PERMISSIONS = "CK"
        const val FIRST_SUFFIX = 2
    }
}
