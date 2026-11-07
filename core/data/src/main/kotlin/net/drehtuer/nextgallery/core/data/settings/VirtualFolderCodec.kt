package net.drehtuer.nextgallery.core.data.settings

import net.drehtuer.nextgallery.core.model.VirtualFolder

/** One stored string per virtual folder: id, bucket ids and name. The name comes last, as it can contain anything. */
internal object VirtualFolderCodec {
    private const val SEPARATOR = ':'
    private const val BUCKET_SEPARATOR = ','
    private const val PARTS = 3

    fun encode(folder: VirtualFolder): String =
        listOf(folder.id, folder.bucketIds.joinToString("$BUCKET_SEPARATOR"), folder.name)
            .joinToString("$SEPARATOR")

    /** Returns null for a value this version cannot read, so that one bad entry does not lose the others. */
    fun decode(value: String): VirtualFolder? = value.split(SEPARATOR, limit = PARTS)
        .takeIf { it.size == PARTS && it.first().isNotEmpty() && it.last().isNotEmpty() }
        ?.let { (id, buckets, name) -> bucketIds(buckets)?.let { VirtualFolder(id, name, it) } }

    private fun bucketIds(text: String): List<Long>? = text.split(BUCKET_SEPARATOR)
        .filter { it.isNotEmpty() }
        .map { it.toLongOrNull() }
        .takeIf { null !in it }
        ?.filterNotNull()
}
