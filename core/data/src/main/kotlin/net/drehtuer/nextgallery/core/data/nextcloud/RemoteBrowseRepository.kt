package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.model.SupportedMimeTypes
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import javax.inject.Inject

/** [items] are the folders and images of [path]; with [error] they are what was loaded before. */
data class RemoteListing(
    val path: String,
    val items: List<RemoteItem>,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
)

interface RemoteBrowseRepository {
    /** What was loaded before first, if anything, then what the server says now. */
    fun list(path: String): Flow<RemoteListing>
}

class DefaultRemoteBrowseRepository @Inject constructor(
    private val runner: RemoteCallRunner,
    private val webDav: WebDavClient,
    private val cache: RemoteCacheStore,
    private val session: AccountSession,
) : RemoteBrowseRepository {
    override fun list(path: String): Flow<RemoteListing> = flow {
        val normalized = RemotePaths.normalize(path)
        val owner = runner.currentAccount()
        if (owner == null) {
            emit(signedOut(normalized))
            return@flow
        }
        val cached = cache.get(normalized)
        emit(RemoteListing(normalized, cached, isRefreshing = true))
        emit(refreshed(owner, normalized, cached))
    }

    /** If [owner] left while waiting for the server, nothing of it is stored or shown under whoever came next. */
    private suspend fun refreshed(owner: NextcloudAccount, path: String, cached: List<RemoteItem>): RemoteListing =
        when (val result = runner.call(owner) { webDav.list(it, path) }) {
            is AppResult.Success -> {
                val items = visible(result.value.children)
                val stored = session.writeFor(owner) { cache.replaceFolder(path, items) }
                if (stored) RemoteListing(path, items) else signedOut(path)
            }

            is AppResult.Failure -> if (result.error == AppError.NotLoggedIn) {
                signedOut(path)
            } else {
                RemoteListing(path, cached, error = result.error)
            }
        }

    private fun signedOut(path: String) = RemoteListing(path, emptyList(), error = AppError.NotLoggedIn)

    /** Folders by name, then the images the app can show, newest first. */
    private fun visible(items: List<RemoteItem>): List<RemoteItem> {
        val (folders, files) = items.partition { it.isDirectory }
        return folders.sortedBy { it.name.lowercase() } +
            files.filter { file -> file.mime?.let(SupportedMimeTypes::isSupported) == true }
                .sortedByDescending { it.lastModifiedMillis }
    }
}
