package net.drehtuer.nextgallery.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.folders.TreeDocumentIds
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.settings.FolderSettingsRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.VirtualFolder
import net.drehtuer.nextgallery.core.model.VirtualFolderSummary
import javax.inject.Inject

/**
 * A virtual folder being created ([id] is null) or edited. [bucketIds] keeps members that are not indexed right now,
 * so that they come back when their folder does.
 */
data class VirtualFolderEditor(val id: String?, val name: String, val bucketIds: Set<Long>) {
    val canSave: Boolean get() = name.isNotBlank() && bucketIds.isNotEmpty()
}

sealed interface FoldersSettingsUiState {
    data object Loading : FoldersSettingsUiState

    /** [folders] are the real folders with images, the ones a virtual folder can combine. */
    data class Content(
        val addedFolders: List<AddedFolder>,
        val virtualFolders: List<VirtualFolderSummary>,
        val folders: List<Folder>,
        val addFailed: Boolean = false,
        val editor: VirtualFolderEditor? = null,
    ) : FoldersSettingsUiState
}

sealed interface FoldersSettingsEvent {
    data class FolderPicked(val treeDocumentId: String) : FoldersSettingsEvent

    data class RemoveFolder(val folder: AddedFolder) : FoldersSettingsEvent

    data object CreateVirtualFolder : FoldersSettingsEvent

    data class EditVirtualFolder(val folder: VirtualFolder) : FoldersSettingsEvent

    data class DeleteVirtualFolder(val id: String) : FoldersSettingsEvent

    data class EditorNameChanged(val name: String) : FoldersSettingsEvent

    data class EditorMemberToggled(val bucketId: Long) : FoldersSettingsEvent

    data object EditorSaved : FoldersSettingsEvent

    data object EditorClosed : FoldersSettingsEvent
}

@HiltViewModel
class FoldersSettingsViewModel @Inject constructor(
    folderRepository: FolderRepository,
    settingsRepository: SettingsRepository,
    private val folderSettings: FolderSettingsRepository,
) : ViewModel() {
    private val addFailed = MutableStateFlow(false)
    private val editor = MutableStateFlow<VirtualFolderEditor?>(null)

    val state: StateFlow<FoldersSettingsUiState> = combine(
        settingsRepository.observeSettings(),
        folderRepository.observeVirtualFolders(),
        folderRepository.observeFolders(),
        addFailed,
        editor,
    ) { settings, virtualFolders, folders, failed, editing ->
        FoldersSettingsUiState.Content(settings.addedFolders, virtualFolders, folders, failed, editing)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FoldersSettingsUiState.Loading)

    fun onEvent(event: FoldersSettingsEvent) {
        when (event) {
            is FoldersSettingsEvent.FolderPicked -> onFolderPicked(event.treeDocumentId)

            is FoldersSettingsEvent.RemoveFolder -> viewModelScope.launch {
                folderSettings.removeFolder(event.folder)
            }

            FoldersSettingsEvent.CreateVirtualFolder -> editor.value = VirtualFolderEditor(null, "", emptySet())

            is FoldersSettingsEvent.EditVirtualFolder ->
                editor.value =
                    VirtualFolderEditor(event.folder.id, event.folder.name, event.folder.bucketIds.toSet())

            is FoldersSettingsEvent.DeleteVirtualFolder -> viewModelScope.launch {
                folderSettings.deleteVirtualFolder(event.id)
            }

            is FoldersSettingsEvent.EditorNameChanged -> editor.update { it?.copy(name = event.name) }

            is FoldersSettingsEvent.EditorMemberToggled -> editor.update { it?.withMemberToggled(event.bucketId) }

            FoldersSettingsEvent.EditorSaved -> saveEditor()

            FoldersSettingsEvent.EditorClosed -> editor.value = null
        }
    }

    private fun onFolderPicked(treeDocumentId: String) {
        val folder = TreeDocumentIds.parse(treeDocumentId)
        addFailed.value = folder == null
        folder?.let { viewModelScope.launch { folderSettings.addFolder(it) } }
    }

    private fun saveEditor() {
        val edited = editor.value?.takeIf { it.canSave } ?: return
        editor.value = null
        val name = edited.name.trim()
        val bucketIds = edited.bucketIds.sorted()
        viewModelScope.launch {
            if (edited.id == null) {
                folderSettings.createVirtualFolder(name, bucketIds)
            } else {
                folderSettings.updateVirtualFolder(VirtualFolder(edited.id, name, bucketIds))
            }
        }
    }

    private fun VirtualFolderEditor.withMemberToggled(bucketId: Long) =
        copy(bucketIds = if (bucketId in bucketIds) bucketIds - bucketId else bucketIds + bucketId)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
