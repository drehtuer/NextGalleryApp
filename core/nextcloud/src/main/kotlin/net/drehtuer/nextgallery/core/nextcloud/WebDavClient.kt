package net.drehtuer.nextgallery.core.nextcloud

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.nextcloud.PropfindRequestBuilder.asXmlBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream

/** A folder and its direct children. [self] is null if the server left the folder itself out. */
data class RemoteFolder(val self: RemoteItem?, val children: List<RemoteItem>)

/**
 * WebDAV on the account's files and trash bin. Uploading stays with the Nextcloud app (D31). Requests that change
 * something use [changeClient], which does not follow redirects, so that a redirect can't repeat them elsewhere.
 */
class WebDavClient(
    private val readClient: OkHttpClient,
    private val changeClient: OkHttpClient,
    private val parser: MultistatusParser = MultistatusParser(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun list(account: NextcloudAccount, path: String): RemoteFolder {
        val normalized = RemotePaths.normalize(path)
        val items = propfindFiles(account, normalized, DEPTH_CHILDREN)
        return RemoteFolder(items.firstOrNull { it.path == normalized }, items.filter { it.path != normalized })
    }

    /** One file or folder; null if the server returned nothing usable for it. */
    suspend fun stat(account: NextcloudAccount, path: String): RemoteItem? {
        val normalized = RemotePaths.normalize(path)
        return propfindFiles(account, normalized, DEPTH_SELF).firstOrNull { it.path == normalized }
    }

    suspend fun download(
        account: NextcloudAccount,
        path: String,
        sink: OutputStream,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Unit = withContext(io) {
        val request = Request.Builder().url(RemotePathMapper.davUrl(account, path)).get().build()
        readClient.newCall(request).await().use { response ->
            response.requireSuccess()
            val total = response.body.contentLength()
            val source = response.body.byteStream()
            val buffer = ByteArray(BUFFER_BYTES)
            var copied = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = source.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
                copied += read
                onProgress(copied, total)
            }
        }
    }

    /** With [overwrite] false (`Overwrite: F`, D46), an existing target fails with 412 and nothing changes. */
    suspend fun move(account: NextcloudAccount, from: String, to: String, overwrite: Boolean = false) = change(
        moveRequest(RemotePathMapper.davUrl(account, from), RemotePathMapper.davUrl(account, to), overwrite),
    )

    suspend fun createFolder(account: NextcloudAccount, path: String) = change(
        Request.Builder().url(RemotePathMapper.davUrl(account, path)).method("MKCOL", null).build(),
    )

    /** Puts the file into the Nextcloud trash bin, if the server has the trash bin enabled. */
    suspend fun delete(account: NextcloudAccount, path: String) = change(
        Request.Builder().url(RemotePathMapper.davUrl(account, path)).delete().build(),
    )

    suspend fun listTrash(account: NextcloudAccount): List<TrashItem> {
        val root = RemotePathMapper.trashRoot(account)
        return propfind(root, DEPTH_CHILDREN, PropfindRequestBuilder.trash()) { stream ->
            parser.parseTrash(stream, root)
        }
    }

    /** Moves the item out of the trash bin; the server puts it back at its original location. */
    suspend fun restore(account: NextcloudAccount, item: TrashItem) = change(
        moveRequest(
            source = RemotePathMapper.trashItemUrl(account, item.trashName),
            destination = RemotePathMapper.restoreUrl(account, item.originalName),
            overwrite = false,
        ),
    )

    private suspend fun change(request: Request): Unit = withContext(io) {
        changeClient.newCall(request).await().use { it.requireSuccess() }
    }

    private suspend fun propfindFiles(account: NextcloudAccount, path: String, depth: String): List<RemoteItem> {
        val root = RemotePathMapper.filesRoot(account)
        return propfind(RemotePathMapper.davUrl(account, path), depth, PropfindRequestBuilder.files()) { stream ->
            parser.parseFiles(stream, root)
        }
    }

    private suspend fun <T> propfind(url: HttpUrl, depth: String, body: String, parse: (InputStream) -> T): T =
        withContext(io) {
            val request = Request.Builder().url(url).method("PROPFIND", body.asXmlBody()).header("Depth", depth).build()
            readClient.newCall(request).await().use { response ->
                response.requireSuccess()
                parse(response.body.byteStream())
            }
        }

    private companion object {
        const val DEPTH_SELF = "0"
        const val DEPTH_CHILDREN = "1"
        const val BUFFER_BYTES = 64 * 1024
    }
}

private fun moveRequest(source: HttpUrl, destination: HttpUrl, overwrite: Boolean): Request = Request.Builder()
    .url(source)
    .method("MOVE", null)
    .header("Destination", destination.toString())
    .header("Overwrite", if (overwrite) "T" else "F")
    .build()
