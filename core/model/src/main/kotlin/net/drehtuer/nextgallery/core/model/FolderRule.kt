package net.drehtuer.nextgallery.core.model

/** Always show (include) or always hide (exclude) a folder and its subfolders. */
data class FolderRule(val relativePath: String, val type: Type) {
    enum class Type { INCLUDE, EXCLUDE }
}
