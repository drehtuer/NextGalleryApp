package net.drehtuer.nextgallery.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.data.upload.UploadHandoffRepository
import net.drehtuer.nextgallery.core.data.upload.UploadStateRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import net.drehtuer.nextgallery.core.model.UploadAvailability
import net.drehtuer.nextgallery.core.model.UploadHandoff
import net.drehtuer.nextgallery.core.model.UploadState

/** [selectedIds] is the app-wide selection, so it can contain images of other folders (D33). */
data class FolderGridUiState(
    val title: String = "",
    /** The members of the virtual folder on screen; null in a real folder. */
    val virtualBucketIds: List<Long>? = null,
    val selectedIds: Set<MediaId> = emptySet(),
    val selecting: Boolean = selectedIds.isNotEmpty(),
    /** The images on screen. An image without an entry has no badge, as all have while logged out (D35). */
    val uploadBadges: Map<MediaId, UploadState> = emptyMap(),
    val uploadAvailability: UploadAvailability = UploadAvailability.HIDDEN,
)

sealed interface FolderGridEvent {
    data class ItemClicked(val id: MediaId) : FolderGridEvent

    data class ItemLongPressed(val id: MediaId) : FolderGridEvent

    /** The Select action: starts selecting before anything is selected. */
    data object StartSelecting : FolderGridEvent

    data object SelectAll : FolderGridEvent

    data object ClearSelection : FolderGridEvent

    data object ShareSelected : FolderGridEvent

    data object UploadSelected : FolderGridEvent

    data object DeleteSelected : FolderGridEvent

    data class ConfirmationResult(val confirmed: Boolean) : FolderGridEvent

    data object UndoDelete : FolderGridEvent

    data class VisibleItemsChanged(val ids: Set<MediaId>) : FolderGridEvent

    /** The screen came back, possibly from the Nextcloud app. */
    data object Resumed : FolderGridEvent
}

sealed interface FolderGridEffect {
    /** [virtualBucketIds] are the members of the virtual folder on screen; null in a real folder. */
    data class OpenViewer(val id: MediaId, val virtualBucketIds: List<Long>? = null) : FolderGridEffect

    data class Share(val spec: ShareSpec) : FolderGridEffect

    data class Upload(val handoff: UploadHandoff) : FolderGridEffect

    data class Trash(val effect: TrashEffect) : FolderGridEffect
}

/** The title and, in a virtual folder, its members. They change while the grid is open when the folder is edited. */
private data class GridHeader(val title: String, val virtualBucketIds: List<Long>?)

@HiltViewModel(assistedFactory = FolderGridViewModel.Factory::class)
class FolderGridViewModel
@Suppress("LongParameterList")
@AssistedInject
constructor(
    @Assisted private val route: FolderGridRoute,
    private val mediaRepository: MediaRepository,
    folderRepository: FolderRepository,
    private val selectionRepository: SelectionRepository,
    private val actions: MediaActionsRepository,
    private val trashFlow: TrashFlow,
    uploadStates: UploadStateRepository,
    private val uploadHandoff: UploadHandoffRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(route: FolderGridRoute): FolderGridViewModel
    }

    private val effectChannel = Channel<FolderGridEffect>(Channel.BUFFERED)
    private val startedSelecting = MutableStateFlow(false)
    private val visibleIds = MutableStateFlow<Set<MediaId>>(emptySet())

    val effects: Flow<FolderGridEffect> = effectChannel.receiveAsFlow()

    private val header: Flow<GridHeader> = route.virtualFolderId?.let { id ->
        folderRepository.observeVirtualFolder(id).map { summary ->
            GridHeader(summary?.folder?.name.orEmpty(), summary?.folder?.bucketIds.orEmpty())
        }
    } ?: folderRepository.observeFolder(route.bucketId).map { GridHeader(it?.name.orEmpty(), null) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<PagingData<MediaItem>> = if (route.virtualFolderId == null) {
        mediaRepository.pagedItems(MediaSource.Folder(route.bucketId))
    } else {
        header.map { it.virtualBucketIds.orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { mediaRepository.pagedItems(MediaSource.VirtualFolder(it)) }
    }.cachedIn(viewModelScope)

    val state: StateFlow<FolderGridUiState> = combine(
        header,
        selectionRepository.observe(),
        startedSelecting,
        uploadStates.observeBadges(visibleIds),
        uploadHandoff.observeAvailability(),
    ) { gridHeader, selectedIds, started, badges, availability ->
        FolderGridUiState(
            title = gridHeader.title,
            virtualBucketIds = gridHeader.virtualBucketIds,
            selectedIds = selectedIds,
            selecting = started || selectedIds.isNotEmpty(),
            uploadBadges = badges,
            uploadAvailability = availability,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FolderGridUiState())

    fun onEvent(event: FolderGridEvent) {
        when (event) {
            is FolderGridEvent.ItemClicked -> onItemClicked(event.id)

            is FolderGridEvent.ItemLongPressed -> selectionRepository.toggle(event.id)

            FolderGridEvent.StartSelecting -> startedSelecting.value = true

            FolderGridEvent.SelectAll -> viewModelScope.launch { selectionRepository.selectAll(allItemIds()) }

            FolderGridEvent.ClearSelection -> {
                startedSelecting.value = false
                selectionRepository.clear()
            }

            FolderGridEvent.ShareSelected -> viewModelScope.launch {
                actions.shareSpec(selectionRepository.observe().value)?.let {
                    effectChannel.send(FolderGridEffect.Share(it))
                }
            }

            FolderGridEvent.UploadSelected -> viewModelScope.launch {
                val handoff = uploadHandoff.prepare(selectionRepository.observe().value)
                effectChannel.send(FolderGridEffect.Upload(handoff))
            }

            is FolderGridEvent.VisibleItemsChanged -> visibleIds.value = event.ids

            FolderGridEvent.Resumed -> uploadHandoff.onAppResumed()

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
        if (!startedSelecting.value && selectionRepository.observe().value.isEmpty()) {
            viewModelScope.launch {
                effectChannel.send(FolderGridEffect.OpenViewer(id, header.first().virtualBucketIds))
            }
        } else {
            selectionRepository.toggle(id)
        }
    }

    private suspend fun allItemIds(): List<MediaId> {
        val virtualBucketIds = header.first().virtualBucketIds
        return if (virtualBucketIds == null) {
            mediaRepository.folderItemIds(route.bucketId)
        } else {
            mediaRepository.virtualFolderItemIds(virtualBucketIds)
        }
    }

    private suspend fun sendTrashEffect(effect: TrashEffect?) {
        effect?.let { effectChannel.send(FolderGridEffect.Trash(it)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
