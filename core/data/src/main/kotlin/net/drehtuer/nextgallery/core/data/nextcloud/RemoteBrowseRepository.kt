package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
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
    private val accounts: AccountStore,
    private val runner: RemoteCallRunner,
    private val webDav: WebDavClient,
    private val cache: RemoteCacheStore,
) : RemoteBrowseRepository {
    override fun list(path: String): Flow<RemoteListing> = flow {
        val normalized = RemotePaths.normalize(path)
        if (accounts.observeAccount().first() == null) {
            emit(RemoteListing(normalized, emptyList(), error = AppError.NotLoggedIn))
            return@flow
        }
        val cached = cache.get(normalized)
        emit(RemoteListing(normalized, cached, isRefreshing = true))
        when (val result = runner.call { account -> webDav.list(account, normalized) }) {
            is AppResult.Success -> {
                val items = visible(result.value.children)
                cache.replaceFolder(normalized, items)
                emit(RemoteListing(normalized, items))
            }

            is AppResult.Failure -> emit(RemoteListing(normalized, cached, error = result.error))
        }
    }

    /** Folders by name, then the images the app can show, newest first. */
    private fun visible(items: List<RemoteItem>): List<RemoteItem> {
        val (folders, files) = items.partition { it.isDirectory }
        return folders.sortedBy { it.name.lowercase() } +
            files.filter { file -> file.mime?.let(SupportedMimeTypes::isSupported) == true }
                .sortedByDescending { it.lastModifiedMillis }
    }
}
