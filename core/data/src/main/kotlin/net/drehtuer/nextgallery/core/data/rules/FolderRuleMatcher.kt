package net.drehtuer.nextgallery.core.data.rules

import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.model.FolderRule

/**
 * Decides whether a folder is indexed: an empty include list means "everything", and an exclude rule always
 * wins over an include rule. Folders with a `.nomedia` file are already hidden by MediaStore.
 */
class FolderRuleMatcher(rules: List<FolderRule>) {
    private val includes = rules.filter { it.type == FolderRule.Type.INCLUDE }.map { it.relativePath }
    private val excludes = rules.filter { it.type == FolderRule.Type.EXCLUDE }.map { it.relativePath }

    fun isIndexed(relativePath: String): Boolean {
        val included = includes.isEmpty() || includes.any { RelativePaths.isUnder(relativePath, it) }
        return included && excludes.none { RelativePaths.isUnder(relativePath, it) }
    }
}
