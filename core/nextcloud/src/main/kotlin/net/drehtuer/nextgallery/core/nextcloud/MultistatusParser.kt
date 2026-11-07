package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths
import okhttp3.HttpUrl
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** An entry of the Nextcloud trash bin. [trashName] is its name in the trash, e.g. "IMG_1.jpg.d1700000000". */
data class TrashItem(
    val trashName: String,
    val originalName: String,
    val originalPath: String,
    val deletionTimeSeconds: Long,
    val fileId: Long?,
)

/**
 * Reads a WebDAV multistatus response. Values of a propstat whose status is not 200 are ignored (the server lists the
 * properties it does not have there). Hrefs outside the root and names no real file can have are dropped: the response
 * comes from the network, and its paths end up in URLs and file names.
 */
class MultistatusParser {
    private val reader = MultistatusReader()

    fun parseFiles(stream: InputStream, root: HttpUrl): List<RemoteItem> = reader.read(stream).mapNotNull { response ->
        relativeSegments(response, root)?.let { toRemoteItem(response, it) }
    }

    fun parseTrash(stream: InputStream, root: HttpUrl): List<TrashItem> = reader.read(stream).mapNotNull { response ->
        relativeSegments(response, root)?.singleOrNull()?.let { toTrashItem(response, it) }
    }

    private fun toRemoteItem(response: RawResponse, segments: List<String>): RemoteItem = RemoteItem(
        path = RemotePaths.normalize(segments.joinToString("/")),
        name = segments.lastOrNull().orEmpty(),
        isDirectory = response.isCollection,
        size = response.properties["getcontentlength"]?.toLongOrNull() ?: 0,
        mime = response.properties["getcontenttype"],
        etag = response.properties["getetag"]?.trim('"'),
        lastModifiedMillis = response.properties["getlastmodified"]?.let(::parseDate) ?: 0,
        fileId = response.properties["fileid"]?.toLongOrNull(),
        checksums = response.properties["checksum"],
        permissions = response.properties["permissions"],
        hasPreview = response.properties["has-preview"] != "false",
        childCount = response.properties["contained-file-count"]?.toIntOrNull(),
    )

    private fun toTrashItem(response: RawResponse, trashName: String): TrashItem? {
        val path = response.properties["trashbin-original-location"]?.let(RemotePaths::normalize)
            ?.takeIf { location -> RemotePaths.segments(location).all(RemotePaths::isValidName) }
        return path?.let {
            TrashItem(
                trashName = trashName,
                originalName = response.properties["trashbin-filename"] ?: RemotePaths.name(path),
                originalPath = path,
                deletionTimeSeconds = response.properties["trashbin-deletion-time"]?.toLongOrNull() ?: 0,
                fileId = response.properties["fileid"]?.toLongOrNull(),
            )
        }
    }

    /** The decoded segments below [root], or null if the href is outside it or contains an impossible name. */
    private fun relativeSegments(response: RawResponse, root: HttpUrl): List<String>? {
        val hasDotDot = response.href.substringBefore('?').split('/')
            .any { it == ".." || it.equals("%2e%2e", ignoreCase = true) }
        val segments = root.resolve(response.href)?.pathSegments.orEmpty().filter { it.isNotEmpty() }
        val rootSegments = root.pathSegments.filter { it.isNotEmpty() }
        val isBelowRoot = !hasDotDot && segments.take(rootSegments.size) == rootSegments
        return segments.drop(rootSegments.size).takeIf { relative ->
            isBelowRoot && relative.all(RemotePaths::isValidName)
        }
    }

    private fun parseDate(value: String): Long? = try {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
