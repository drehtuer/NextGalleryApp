package net.drehtuer.nextgallery.core.model

/**
 * A file or folder on the server. [path] is decoded and relative to the user's files: "/Photos/IMG 1.jpg".
 * [permissions] is the raw `oc:permissions` value and [checksums] the raw `oc:checksums` value (null if not sent).
 */
data class RemoteItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val mime: String? = null,
    val etag: String? = null,
    val lastModifiedMillis: Long = 0,
    val fileId: Long? = null,
    val checksums: String? = null,
    val permissions: String? = null,
    val hasPreview: Boolean = true,
    val childCount: Int? = null,
) {
    val parentPath: String get() = RemotePaths.parent(path)
}
