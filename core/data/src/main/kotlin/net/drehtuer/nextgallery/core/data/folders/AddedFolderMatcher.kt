package net.drehtuer.nextgallery.core.data.folders

import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.model.AddedFolder

/**
 * Decides whether an image is indexed (D55): it has to be in an added folder or below one, and no folder below the
 * added one may start with a dot. Folders with a `.nomedia` file are already hidden by MediaStore.
 */
class AddedFolderMatcher(folders: List<AddedFolder>) {
    private val pathsByVolume = folders.groupBy({ it.volumeName }, { RelativePaths.normalize(it.relativePath) })

    /** The added folders on [volume]. */
    fun pathsOn(volume: String): List<String> = pathsByVolume[volume].orEmpty()

    fun isIndexed(volume: String, relativePath: String): Boolean {
        val path = RelativePaths.normalize(relativePath)
        return pathsOn(volume).any { RelativePaths.isUnder(path, it) && !hasHiddenFolderBelow(path, it) }
    }

    private fun hasHiddenFolderBelow(path: String, folder: String) =
        path.drop(folder.length).split('/').any { it.startsWith('.') }
}
