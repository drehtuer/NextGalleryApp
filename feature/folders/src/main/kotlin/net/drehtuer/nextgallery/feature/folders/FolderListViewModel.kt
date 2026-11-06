package net.drehtuer.nextgallery.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.model.Folder
import javax.inject.Inject

sealed interface FolderListUiState {
    data object Loading : FolderListUiState

    data object Empty : FolderListUiState

    data class Content(val folders: List<Folder>) : FolderListUiState
}

@HiltViewModel
class FolderListViewModel @Inject constructor(folderRepository: FolderRepository) : ViewModel() {
    val state: StateFlow<FolderListUiState> = folderRepository.observeFolders()
        .map { folders -> if (folders.isEmpty()) FolderListUiState.Empty else FolderListUiState.Content(folders) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FolderListUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
