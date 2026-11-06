package net.drehtuer.nextgallery.core.model

/** Include or exclude a folder (and its subfolders) from indexing. */
data class FolderRule(val relativePath: String, val type: Type) {
    enum class Type { INCLUDE, EXCLUDE }
}
