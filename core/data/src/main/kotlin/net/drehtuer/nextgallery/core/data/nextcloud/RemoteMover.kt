package net.drehtuer.nextgallery.core.data.nextcloud

import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient

/** Moves one image. It always tries without overwriting first (D46); only the user's answer can change that. */
internal class RemoteMover(
    private val runner: RemoteCallRunner,
    private val webDav: WebDavClient,
    private val nextFreeName: (String, Set<String>) -> String,
) {
    suspend fun move(item: RemoteItem, target: String, policy: ConflictPolicy): RemoteMoveResult {
        if (item.parentPath == target) return RemoteMoveResult.Skipped(item)
        val targetPath = RemotePaths.join(target, item.name)
        return when (val result = moveTo(item, targetPath)) {
            is AppResult.Success -> RemoteMoveResult.Moved(item, targetPath)

            is AppResult.Failure -> if (result.error == AppError.Conflict) {
                resolveConflict(item, target, targetPath, policy)
            } else {
                RemoteMoveResult.Failed(item, result.error)
            }
        }
    }

    private suspend fun moveTo(item: RemoteItem, path: String, overwrite: Boolean = false): AppResult<Unit> =
        runner.call { webDav.move(it, item.path, path, overwrite) }

    private suspend fun resolveConflict(
        item: RemoteItem,
        target: String,
        targetPath: String,
        policy: ConflictPolicy,
    ): RemoteMoveResult = when (policy) {
        ConflictPolicy.ASK -> RemoteMoveResult.Conflict(item, existingAt(targetPath))
        ConflictPolicy.SKIP -> RemoteMoveResult.Skipped(item)
        ConflictPolicy.REPLACE -> replace(item, targetPath)
        ConflictPolicy.KEEP_BOTH -> keepBoth(item, target)
    }

    private suspend fun existingAt(path: String): RemoteItem? =
        (runner.call { webDav.stat(it, path) } as? AppResult.Success)?.value

    /** Overwriting a folder would delete everything in it, so only a file can be replaced. */
    private suspend fun replace(item: RemoteItem, targetPath: String): RemoteMoveResult =
        when (val existing = runner.call { webDav.stat(it, targetPath) }) {
            is AppResult.Failure -> RemoteMoveResult.Failed(item, existing.error)

            is AppResult.Success -> if (existing.value?.isDirectory == false) {
                when (val result = moveTo(item, targetPath, overwrite = true)) {
                    is AppResult.Success -> RemoteMoveResult.Replaced(item, targetPath)
                    is AppResult.Failure -> RemoteMoveResult.Failed(item, result.error)
                }
            } else {
                RemoteMoveResult.Failed(item, AppError.Conflict)
            }
        }

    private suspend fun keepBoth(item: RemoteItem, target: String): RemoteMoveResult =
        when (val listing = runner.call { webDav.list(it, target) }) {
            is AppResult.Failure -> RemoteMoveResult.Failed(item, listing.error)
            is AppResult.Success -> moveUnderFreeName(item, target, listing.value.children.map { it.name }.toSet())
        }

    /** If someone else takes the name in the meantime, the server answers with a conflict and the next one is tried. */
    private suspend fun moveUnderFreeName(item: RemoteItem, target: String, names: Set<String>): RemoteMoveResult {
        val taken = names.toMutableSet()
        var outcome: RemoteMoveResult = RemoteMoveResult.Failed(item, AppError.Conflict)
        var attempts = 0
        while (attempts < KEEP_BOTH_ATTEMPTS && outcome is RemoteMoveResult.Failed &&
            outcome.error == AppError.Conflict
        ) {
            val name = nextFreeName(item.name, taken)
            val newPath = RemotePaths.join(target, name)
            outcome = when (val result = moveTo(item, newPath)) {
                is AppResult.Success -> RemoteMoveResult.Renamed(item, newPath)
                is AppResult.Failure -> RemoteMoveResult.Failed(item, result.error)
            }
            taken += name
            attempts++
        }
        return outcome
    }

    private companion object {
        const val KEEP_BOTH_ATTEMPTS = 5
    }
}
