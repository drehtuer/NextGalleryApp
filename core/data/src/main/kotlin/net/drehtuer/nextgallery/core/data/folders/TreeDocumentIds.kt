package net.drehtuer.nextgallery.core.data.folders

import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.model.AddedFolder

/** Reads the document id of the system folder picker, which is `<storage>:<path>` for the storages on the phone. */
object TreeDocumentIds {
    private const val PRIMARY_STORAGE = "primary"
    private val STORAGE_ID = Regex("[0-9a-fA-F-]+")

    /** Returns null for a storage's root, and for anything else that is not a folder on a storage. */
    fun parse(documentId: String): AddedFolder? {
        val storage = documentId.substringBefore(':', missingDelimiterValue = "")
        val path = RelativePaths.normalize(documentId.substringAfter(':', missingDelimiterValue = ""))
        return when {
            path.isEmpty() -> null
            storage == PRIMARY_STORAGE -> AddedFolder(AddedFolder.PRIMARY_VOLUME, path)
            STORAGE_ID.matches(storage) -> AddedFolder(storage.lowercase(), path)
            else -> null
        }
    }
}
