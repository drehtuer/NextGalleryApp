package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.nextcloud.WebDavClient
import javax.inject.Inject

sealed interface RemoteShareProgress {
    /** Image [current] of [total] is being downloaded. */
    data class Downloading(val current: Int, val total: Int) : RemoteShareProgress

    data class Ready(val spec: ShareSpec) : RemoteShareProgress

    data class Failed(val error: AppError) : RemoteShareProgress
}

/** Server images have no local file, so sharing downloads them to a temporary folder first (D48). */
interface RemoteShareRepository {
    fun prepare(items: List<RemoteItem>): Flow<RemoteShareProgress>

    /** Removes the temporary files; runs at each share and when the app starts. */
    fun clear()
}

class DefaultRemoteShareRepository(
    private val runner: RemoteCallRunner,
    private val webDav: WebDavClient,
    private val cache: RemoteShareCache,
    private val connectivity: ConnectivitySource,
    private val io: CoroutineDispatcher,
) : RemoteShareRepository {
    @Inject
    constructor(
        runner: RemoteCallRunner,
        webDav: WebDavClient,
        cache: RemoteShareCache,
        connectivity: ConnectivitySource,
    ) : this(runner, webDav, cache, connectivity, Dispatchers.IO)

    override fun prepare(items: List<RemoteItem>): Flow<RemoteShareProgress> = flow {
        cache.clear()
        if (!connectivity.isOnline.first()) {
            emit(RemoteShareProgress.Failed(AppError.Network(null)))
            return@flow
        }
        val uris = mutableListOf<String>()
        items.forEachIndexed { index, item ->
            emit(RemoteShareProgress.Downloading(index + 1, items.size))
            val file = cache.newFile(index, safeFileName(item.name))
            val result = runner.call { account ->
                file.outputStream().use { sink -> webDav.download(account, item.path, sink) }
            }
            if (result is AppResult.Failure) {
                cache.clear()
                emit(RemoteShareProgress.Failed(result.error))
                return@flow
            }
            uris += cache.contentUri(file)
        }
        emit(RemoteShareProgress.Ready(ShareSpec(uris, shareMimeType(items))))
    }.flowOn(io)

    override fun clear() = cache.clear()

    private fun shareMimeType(items: List<RemoteItem>): String =
        items.map { it.mime }.distinct().singleOrNull() ?: ANY_IMAGE

    private companion object {
        const val ANY_IMAGE = "image/*"
    }
}

/**
 * The name comes from the server, so it must not be able to leave the temporary folder or hide as a dot file. Only the
 * last path part is kept.
 */
internal fun safeFileName(name: String): String {
    val last = name.substringAfterLast('/').substringAfterLast('\\').trim()
    return last.trimStart('.').filterNot { it.isISOControl() }.ifEmpty { "image" }
}
