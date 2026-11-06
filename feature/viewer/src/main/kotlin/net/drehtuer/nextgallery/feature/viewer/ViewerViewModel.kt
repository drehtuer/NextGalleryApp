package net.drehtuer.nextgallery.feature.viewer

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.model.ImageDetails
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect

/** [initialIndex] is null until the position of the opened image is known. */
data class ViewerUiState(
    val initialIndex: Int? = null,
    val current: MediaItem? = null,
    val chromeVisible: Boolean = true,
    val details: ImageDetails? = null,
)

sealed interface ViewerEvent {
    data class PageChanged(val item: MediaItem?) : ViewerEvent

    /** The number of images after a finished load; the list can shrink while the viewer is open. */
    data class ItemCountChanged(val count: Int) : ViewerEvent

    data object ToggleChrome : ViewerEvent

    data object ShowInfo : ViewerEvent

    data object HideInfo : ViewerEvent

    data object Share : ViewerEvent

    data object Delete : ViewerEvent

    data class ConfirmationResult(val confirmed: Boolean) : ViewerEvent

    data object UndoDelete : ViewerEvent
}

sealed interface ViewerEffect {
    data class Share(val spec: ShareSpec) : ViewerEffect

    data class Trash(val effect: TrashEffect) : ViewerEffect

    data object Close : ViewerEffect
}

@HiltViewModel(assistedFactory = ViewerViewModel.Factory::class)
class ViewerViewModel @AssistedInject constructor(
    @Assisted route: ViewerRoute,
    private val mediaRepository: MediaRepository,
    private val actions: MediaActionsRepository,
    private val trashFlow: TrashFlow,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(route: ViewerRoute): ViewerViewModel
    }

    private val source = MediaSource.Folder(route.bucketId)
    private val mutableState = MutableStateFlow(ViewerUiState())
    private val effectChannel = Channel<ViewerEffect>(Channel.BUFFERED)

    val state: StateFlow<ViewerUiState> = mutableState.asStateFlow()

    val effects: Flow<ViewerEffect> = effectChannel.receiveAsFlow()

    val items: Flow<PagingData<MediaItem>> = mediaRepository.pagedItems(source).cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            val index = mediaRepository.indexOf(source, MediaId(route.mediaId))
            mutableState.update { it.copy(initialIndex = index) }
        }
    }

    fun onEvent(event: ViewerEvent) {
        when (event) {
            is ViewerEvent.PageChanged -> mutableState.update { it.copy(current = event.item) }

            is ViewerEvent.ItemCountChanged -> if (event.count == 0) effectChannel.trySend(ViewerEffect.Close)

            ViewerEvent.ToggleChrome -> mutableState.update { it.copy(chromeVisible = !it.chromeVisible) }

            ViewerEvent.ShowInfo -> withCurrent { id ->
                val details = mediaRepository.imageDetails(id)
                mutableState.update { it.copy(details = details) }
            }

            ViewerEvent.HideInfo -> mutableState.update { it.copy(details = null) }

            ViewerEvent.Share -> withCurrent { id ->
                actions.shareSpec(listOf(id))?.let { effectChannel.send(ViewerEffect.Share(it)) }
            }

            ViewerEvent.Delete -> withCurrent { id -> sendTrashEffect(trashFlow.request(listOf(id))) }

            is ViewerEvent.ConfirmationResult -> viewModelScope.launch {
                sendTrashEffect(trashFlow.onConfirmationResult(event.confirmed))
            }

            ViewerEvent.UndoDelete -> viewModelScope.launch { sendTrashEffect(trashFlow.undo()) }
        }
    }

    private fun withCurrent(action: suspend (MediaId) -> Unit) {
        val id = mutableState.value.current?.id ?: return
        viewModelScope.launch { action(id) }
    }

    private suspend fun sendTrashEffect(effect: TrashEffect?) {
        effect?.let { effectChannel.send(ViewerEffect.Trash(it)) }
    }
}
