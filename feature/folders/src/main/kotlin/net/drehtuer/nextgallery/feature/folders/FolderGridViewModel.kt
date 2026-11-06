package net.drehtuer.nextgallery.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect

/** [selectedIds] is the app-wide selection, so it can contain images of other folders (D33). */
data class FolderGridUiState(val title: String = "", val selectedIds: Set<MediaId> = emptySet())

sealed interface FolderGridEvent {
    data class ItemClicked(val id: MediaId) : FolderGridEvent

    data class ItemLongPressed(val id: MediaId) : FolderGridEvent

    data object SelectAll : FolderGridEvent

    data object ClearSelection : FolderGridEvent

    data object ShareSelected : FolderGridEvent

    data object DeleteSelected : FolderGridEvent

    data class ConfirmationResult(val confirmed: Boolean) : FolderGridEvent

    data object UndoDelete : FolderGridEvent
}

sealed interface FolderGridEffect {
    data class OpenViewer(val id: MediaId) : FolderGridEffect

    data class Share(val spec: ShareSpec) : FolderGridEffect

    data class Trash(val effect: TrashEffect) : FolderGridEffect
}

@HiltViewModel(assistedFactory = FolderGridViewModel.Factory::class)
class FolderGridViewModel @AssistedInject constructor(
    @Assisted private val bucketId: Long,
    private val mediaRepository: MediaRepository,
    folderRepository: FolderRepository,
    private val selectionRepository: SelectionRepository,
    private val actions: MediaActionsRepository,
    private val trashFlow: TrashFlow,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(bucketId: Long): FolderGridViewModel
    }

    private val effectChannel = Channel<FolderGridEffect>(Channel.BUFFERED)

    val effects: Flow<FolderGridEffect> = effectChannel.receiveAsFlow()

    val items: Flow<PagingData<MediaItem>> =
        mediaRepository.pagedItems(MediaSource.Folder(bucketId)).cachedIn(viewModelScope)

    val state: StateFlow<FolderGridUiState> = combine(
        folderRepository.observeFolder(bucketId),
        selectionRepository.observe(),
    ) { folder, selectedIds -> FolderGridUiState(folder?.name.orEmpty(), selectedIds) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FolderGridUiState())

    fun onEvent(event: FolderGridEvent) {
        when (event) {
            is FolderGridEvent.ItemClicked -> onItemClicked(event.id)

            is FolderGridEvent.ItemLongPressed -> selectionRepository.toggle(event.id)

            FolderGridEvent.SelectAll -> viewModelScope.launch {
                selectionRepository.selectAll(mediaRepository.folderItemIds(bucketId))
            }

            FolderGridEvent.ClearSelection -> selectionRepository.clear()

            FolderGridEvent.ShareSelected -> viewModelScope.launch {
                actions.shareSpec(selectionRepository.observe().value)?.let {
                    effectChannel.send(FolderGridEffect.Share(it))
                }
            }

            FolderGridEvent.DeleteSelected -> viewModelScope.launch {
                sendTrashEffect(trashFlow.request(selectionRepository.observe().value))
            }

            is FolderGridEvent.ConfirmationResult -> viewModelScope.launch {
                sendTrashEffect(trashFlow.onConfirmationResult(event.confirmed))
            }

            FolderGridEvent.UndoDelete -> viewModelScope.launch { sendTrashEffect(trashFlow.undo()) }
        }
    }

    /** During a selection a click changes the selection; otherwise it opens the image. */
    private fun onItemClicked(id: MediaId) {
        if (selectionRepository.observe().value.isEmpty()) {
            effectChannel.trySend(FolderGridEffect.OpenViewer(id))
        } else {
            selectionRepository.toggle(id)
        }
    }

    private suspend fun sendTrashEffect(effect: TrashEffect?) {
        effect?.let { effectChannel.send(FolderGridEffect.Trash(it)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
