package net.drehtuer.nextgallery.core.data.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.VirtualFolder
import net.drehtuer.nextgallery.core.model.VirtualFolderSummary
import javax.inject.Inject

interface FolderRepository {
    fun observeFolders(): Flow<List<Folder>>

    /** Emits null when the folder has no indexed images. */
    fun observeFolder(bucketId: Long): Flow<Folder?>

    /** All virtual folders, also those without an image right now (D55). */
    fun observeVirtualFolders(): Flow<List<VirtualFolderSummary>>

    /** Emits null when there is no such virtual folder. */
    fun observeVirtualFolder(id: String): Flow<VirtualFolderSummary?>
}

class DefaultFolderRepository @Inject constructor(
    private val indexStore: MediaIndexStore,
    private val settingsRepository: SettingsRepository,
) : FolderRepository {
    override fun observeFolders(): Flow<List<Folder>> = indexStore.observeFolders()

    override fun observeFolder(bucketId: Long): Flow<Folder?> =
        indexStore.observeFolders().map { folders -> folders.find { it.bucketId == bucketId } }

    override fun observeVirtualFolders(): Flow<List<VirtualFolderSummary>> = combine(
        settingsRepository.observeSettings().map { it.virtualFolders }.distinctUntilChanged(),
        indexStore.observeFolders(),
    ) { virtualFolders, folders -> virtualFolders.map { it.summary(folders) } }

    override fun observeVirtualFolder(id: String): Flow<VirtualFolderSummary?> =
        observeVirtualFolders().map { summaries -> summaries.find { it.folder.id == id } }

    private fun VirtualFolder.summary(folders: List<Folder>): VirtualFolderSummary {
        val members = folders.filter { it.bucketId in bucketIds }
        return VirtualFolderSummary(
            folder = this,
            itemCount = members.sumOf { it.itemCount },
            // The same order as the images in the grid: newest first, the higher id first on the same date.
            coverItem = members.map { it.coverItem }.maxWithOrNull(compareBy({ it.dateTakenMillis }, { it.id.value })),
        )
    }
}
