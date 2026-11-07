package net.drehtuer.nextgallery.core.data.settings

import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.VirtualFolder
import java.util.UUID
import javax.inject.Inject

/** Changes the folders in [UserSettings] (D55). They are read through [SettingsRepository.observeSettings]. */
interface FolderSettingsRepository {
    /** Adding a folder that is already added changes nothing. */
    suspend fun addFolder(folder: AddedFolder): AppResult<Unit>

    suspend fun removeFolder(folder: AddedFolder): AppResult<Unit>

    suspend fun createVirtualFolder(name: String, bucketIds: List<Long>): AppResult<Unit>

    /** Replaces the virtual folder with the same id. */
    suspend fun updateVirtualFolder(folder: VirtualFolder): AppResult<Unit>

    suspend fun deleteVirtualFolder(id: String): AppResult<Unit>
}

class DefaultFolderSettingsRepository @Inject constructor(private val dataSource: SettingsDataSource) :
    FolderSettingsRepository {
    override suspend fun addFolder(folder: AddedFolder): AppResult<Unit> = update { settings ->
        if (settings.addedFolders.any { it.isSameFolderAs(folder) }) {
            settings
        } else {
            val added = folder.copy(relativePath = RelativePaths.normalize(folder.relativePath))
            settings.copy(addedFolders = settings.addedFolders + added)
        }
    }

    override suspend fun removeFolder(folder: AddedFolder): AppResult<Unit> = update { settings ->
        settings.copy(addedFolders = settings.addedFolders.filterNot { it.isSameFolderAs(folder) })
    }

    override suspend fun createVirtualFolder(name: String, bucketIds: List<Long>): AppResult<Unit> =
        update { settings ->
            val folder = VirtualFolder(UUID.randomUUID().toString(), name, bucketIds)
            settings.copy(virtualFolders = settings.virtualFolders + folder)
        }

    override suspend fun updateVirtualFolder(folder: VirtualFolder): AppResult<Unit> = update { settings ->
        settings.copy(virtualFolders = settings.virtualFolders.map { if (it.id == folder.id) folder else it })
    }

    override suspend fun deleteVirtualFolder(id: String): AppResult<Unit> = update { settings ->
        settings.copy(virtualFolders = settings.virtualFolders.filterNot { it.id == id })
    }

    private suspend fun update(transform: (UserSettings) -> UserSettings): AppResult<Unit> =
        runCatchingCancellable { dataSource.update(transform) }
}

/** Paths are compared without case, because shared storage is case-insensitive. */
private fun AddedFolder.isSameFolderAs(other: AddedFolder) = volumeName == other.volumeName &&
    RelativePaths.isUnder(relativePath, other.relativePath) &&
    RelativePaths.isUnder(other.relativePath, relativePath)
