package net.drehtuer.nextgallery.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.VirtualFolderSummary
import javax.inject.Inject

sealed interface FolderListUiState {
    data object Loading : FolderListUiState

    /** The user has not added a folder yet (D55). */
    data object NoFolders : FolderListUiState

    /** There are added folders, but no image in them: they are empty, or the first scan is still running. */
    data object NoImages : FolderListUiState

    /** [virtualFolders] only has those with at least one image. */
    data class Content(val folders: List<Folder>, val virtualFolders: List<VirtualFolderSummary> = emptyList()) :
        FolderListUiState
}

@HiltViewModel
class FolderListViewModel @Inject constructor(
    folderRepository: FolderRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<FolderListUiState> = combine(
        folderRepository.observeFolders(),
        folderRepository.observeVirtualFolders(),
        settingsRepository.observeSettings().map { it.addedFolders.isEmpty() }.distinctUntilChanged(),
    ) { folders, virtualFolders, noFoldersAdded ->
        when {
            folders.isNotEmpty() -> FolderListUiState.Content(folders, virtualFolders.filter { it.itemCount > 0 })
            noFoldersAdded -> FolderListUiState.NoFolders
            else -> FolderListUiState.NoImages
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FolderListUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
