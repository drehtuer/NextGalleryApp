package net.drehtuer.nextgallery.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionOverviewItem
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import net.drehtuer.nextgallery.core.model.ViewContext

/** [items] is null while the selected images are being read. */
data class SelectionOverviewUiState(
    val items: List<SelectionOverviewItem>? = null,
    val greyedOutForMissingLocation: Boolean = false,
)

sealed interface SelectionOverviewEvent {
    data class ItemClicked(val id: MediaId) : SelectionOverviewEvent

    data object Share : SelectionOverviewEvent

    data object Delete : SelectionOverviewEvent

    data class ConfirmationResult(val confirmed: Boolean) : SelectionOverviewEvent

    data object UndoDelete : SelectionOverviewEvent
}

sealed interface SelectionOverviewEffect {
    data class Share(val spec: ShareSpec) : SelectionOverviewEffect

    data class Trash(val effect: TrashEffect) : SelectionOverviewEffect
}

/** All selected images (D33); those the calling view cannot show are greyed out. */
@HiltViewModel(assistedFactory = SelectionOverviewViewModel.Factory::class)
class SelectionOverviewViewModel @AssistedInject constructor(
    @Assisted context: ViewContext,
    private val selectionRepository: SelectionRepository,
    private val actions: MediaActionsRepository,
    private val trashFlow: TrashFlow,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(context: ViewContext): SelectionOverviewViewModel
    }

    private val effectChannel = Channel<SelectionOverviewEffect>(Channel.BUFFERED)

    val effects: Flow<SelectionOverviewEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<SelectionOverviewUiState> = selectionRepository.observeOverview(context)
        .map { SelectionOverviewUiState(it, greyedOutForMissingLocation = context == ViewContext.Map) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SelectionOverviewUiState())

    fun onEvent(event: SelectionOverviewEvent) {
        when (event) {
            is SelectionOverviewEvent.ItemClicked -> selectionRepository.toggle(event.id)

            SelectionOverviewEvent.Share -> viewModelScope.launch {
                actions.shareSpec(selectionRepository.observe().value)?.let {
                    effectChannel.send(SelectionOverviewEffect.Share(it))
                }
            }

            SelectionOverviewEvent.Delete -> viewModelScope.launch {
                sendTrashEffect(trashFlow.request(selectionRepository.observe().value))
            }

            is SelectionOverviewEvent.ConfirmationResult -> viewModelScope.launch {
                sendTrashEffect(trashFlow.onConfirmationResult(event.confirmed))
            }

            SelectionOverviewEvent.UndoDelete -> viewModelScope.launch { sendTrashEffect(trashFlow.undo()) }
        }
    }

    private suspend fun sendTrashEffect(effect: TrashEffect?) {
        effect?.let { effectChannel.send(SelectionOverviewEffect.Trash(it)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
