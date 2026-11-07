package net.drehtuer.nextgallery.core.data.settings

import net.drehtuer.nextgallery.core.model.AddedFolder

/** One stored string per folder: the volume, a separator and the path. */
internal object AddedFolderCodec {
    private const val SEPARATOR = ':'

    fun encode(folder: AddedFolder): String = "${folder.volumeName}$SEPARATOR${folder.relativePath}"

    /** Returns null for a value this version cannot read, so that one bad entry does not lose the others. */
    fun decode(value: String): AddedFolder? {
        val volume = value.substringBefore(SEPARATOR, missingDelimiterValue = "")
        val path = value.substringAfter(SEPARATOR, missingDelimiterValue = "")
        return if (volume.isEmpty() || path.isEmpty()) null else AddedFolder(volume, path)
    }
}
