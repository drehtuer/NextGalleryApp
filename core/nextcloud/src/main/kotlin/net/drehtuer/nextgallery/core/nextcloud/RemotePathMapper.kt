package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.RemotePaths
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Builds the server's URLs. Segments are encoded here; a dot segment is refused: it would climb out of the root. */
object RemotePathMapper {
    private const val DAV = "remote.php/dav"
    private const val PREVIEW = "index.php/core/preview"
    private val DOT_SEGMENTS = setOf(".", "..")

    fun filesRoot(account: NextcloudAccount): HttpUrl = davRoot(account, "files").build()

    fun davUrl(account: NextcloudAccount, path: String): HttpUrl =
        davRoot(account, "files").addSafeSegments(RemotePaths.segments(path)).build()

    fun previewUrl(account: NextcloudAccount, fileId: Long, size: Int): HttpUrl = account.serverUrl.toHttpUrl()
        .newBuilder()
        .addPathSegments(PREVIEW)
        .addQueryParameter("fileId", fileId.toString())
        .addQueryParameter("x", size.toString())
        .addQueryParameter("y", size.toString())
        .addQueryParameter("a", "1")
        .build()

    fun trashRoot(account: NextcloudAccount): HttpUrl = davRoot(account, "trashbin").addPathSegment("trash").build()

    fun trashItemUrl(account: NextcloudAccount, trashName: String): HttpUrl =
        trashRoot(account).newBuilder().addSafeSegments(listOf(trashName)).build()

    /** A trash-bin item is restored by moving it into this virtual folder; the server puts it back where it was. */
    fun restoreUrl(account: NextcloudAccount, name: String): HttpUrl =
        davRoot(account, "trashbin").addPathSegment("restore").addSafeSegments(listOf(name)).build()

    private fun davRoot(account: NextcloudAccount, space: String): HttpUrl.Builder = account.serverUrl.toHttpUrl()
        .newBuilder()
        .addPathSegments(DAV)
        .addPathSegment(space)
        .addPathSegment(account.userId)

    private fun HttpUrl.Builder.addSafeSegments(segments: List<String>): HttpUrl.Builder = apply {
        segments.forEach { segment ->
            require(segment !in DOT_SEGMENTS) { "Dot segments are not allowed in a remote path" }
            addPathSegment(segment)
        }
    }
}
