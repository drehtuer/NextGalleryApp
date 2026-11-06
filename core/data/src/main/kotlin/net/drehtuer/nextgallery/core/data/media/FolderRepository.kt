package net.drehtuer.nextgallery.core.data.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.model.Folder
import javax.inject.Inject

interface FolderRepository {
    fun observeFolders(): Flow<List<Folder>>

    /** Emits null when the folder has no indexed images. */
    fun observeFolder(bucketId: Long): Flow<Folder?>
}

class DefaultFolderRepository @Inject constructor(private val indexStore: MediaIndexStore) : FolderRepository {
    override fun observeFolders(): Flow<List<Folder>> = indexStore.observeFolders()

    override fun observeFolder(bucketId: Long): Flow<Folder?> =
        indexStore.observeFolders().map { folders -> folders.find { it.bucketId == bucketId } }
}
