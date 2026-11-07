package net.drehtuer.nextgallery.core.data.rules

import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.model.FolderRule

/**
 * Decides whether a folder is indexed (D52). The rule nearest to the folder decides, on the folder itself or else on
 * its closest parent. Without a rule the folder is indexed unless it matches an exclude pattern, so "Include always
 * shows it, Exclude always hides it". Folders with a `.nomedia` file are already hidden by MediaStore.
 */
class FolderRuleMatcher(rules: List<FolderRule>, excludePatterns: List<String> = emptyList()) {
    private val rules = rules.map { it.copy(relativePath = RelativePaths.normalize(it.relativePath)) }
    private val patterns = excludePatterns.map(ExcludePatterns::toRegex)

    fun isIndexed(relativePath: String): Boolean {
        val path = RelativePaths.normalize(relativePath)
        val nearestRule = rules
            .filter { RelativePaths.isUnder(path, it.relativePath) }
            .maxWithOrNull(compareBy({ it.relativePath.length }, { it.type == FolderRule.Type.EXCLUDE }))
        return when (nearestRule?.type) {
            FolderRule.Type.INCLUDE -> true
            FolderRule.Type.EXCLUDE -> false
            null -> patterns.none { it.matches(path) }
        }
    }
}
