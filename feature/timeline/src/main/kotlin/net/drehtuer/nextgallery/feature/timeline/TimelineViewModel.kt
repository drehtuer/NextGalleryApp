package net.drehtuer.nextgallery.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.data.timeline.TimelineIndex
import net.drehtuer.nextgallery.core.data.timeline.TimelineRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** [index] is null until the layout is known. [selectedIds] is the app-wide selection (D33). */
data class TimelineUiState(
    val index: TimelineIndex? = null,
    val today: LocalDate? = null,
    val selectedIds: Set<MediaId> = emptySet(),
    val selecting: Boolean = selectedIds.isNotEmpty(),
)

sealed interface TimelineEvent {
    data class ItemClicked(val id: MediaId) : TimelineEvent

    data class ItemLongPressed(val id: MediaId) : TimelineEvent

    /** The Select action: starts selecting before anything is selected. */
    data object StartSelecting : TimelineEvent

    data object SelectAll : TimelineEvent

    data object ClearSelection : TimelineEvent

    data object ShareSelected : TimelineEvent

    data object DeleteSelected : TimelineEvent

    data class ConfirmationResult(val confirmed: Boolean) : TimelineEvent

    data object UndoDelete : TimelineEvent
}

sealed interface TimelineEffect {
    data class OpenViewer(val id: MediaId) : TimelineEffect

    data class Share(val spec: ShareSpec) : TimelineEffect

    data class Trash(val effect: TrashEffect) : TimelineEffect
}

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    timelineRepository: TimelineRepository,
    private val selectionRepository: SelectionRepository,
    private val actions: MediaActionsRepository,
    private val trashFlow: TrashFlow,
    private val clock: Clock,
) : ViewModel() {
    private val effectChannel = Channel<TimelineEffect>(Channel.BUFFERED)
    private val startedSelecting = MutableStateFlow(false)

    val effects: Flow<TimelineEffect> = effectChannel.receiveAsFlow()

    /** The same list the viewer pages through, so a photo's position in it is the viewer's position too. */
    val items: Flow<PagingData<MediaItem>> = mediaRepository.pagedItems(MediaSource.Timeline).cachedIn(viewModelScope)

    /** Today is read with each update, and again when the screen returns, so "Today" moves on overnight. */
    val state: StateFlow<TimelineUiState> = combine(
        timelineRepository.observeIndex(),
        selectionRepository.observe(),
        startedSelecting,
    ) { index, selectedIds, started ->
        TimelineUiState(index, today(), selectedIds, selecting = started || selectedIds.isNotEmpty())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TimelineUiState())

    fun onEvent(event: TimelineEvent) {
        when (event) {
            is TimelineEvent.ItemClicked -> onItemClicked(event.id)

            is TimelineEvent.ItemLongPressed -> selectionRepository.toggle(event.id)

            TimelineEvent.StartSelecting -> startedSelecting.value = true

            TimelineEvent.SelectAll -> viewModelScope.launch {
                selectionRepository.selectAll(mediaRepository.timelineItemIds())
            }

            TimelineEvent.ClearSelection -> {
                startedSelecting.value = false
                selectionRepository.clear()
            }

            TimelineEvent.ShareSelected -> viewModelScope.launch {
                actions.shareSpec(selectionRepository.observe().value)?.let {
                    effectChannel.send(TimelineEffect.Share(it))
                }
            }

            TimelineEvent.DeleteSelected -> viewModelScope.launch {
                sendTrashEffect(trashFlow.request(selectionRepository.observe().value))
            }

            is TimelineEvent.ConfirmationResult -> viewModelScope.launch {
                sendTrashEffect(trashFlow.onConfirmationResult(event.confirmed))
            }

            TimelineEvent.UndoDelete -> viewModelScope.launch { sendTrashEffect(trashFlow.undo()) }
        }
    }

    /** During a selection a click changes the selection; otherwise it opens the image. */
    private fun onItemClicked(id: MediaId) {
        if (!startedSelecting.value && selectionRepository.observe().value.isEmpty()) {
            effectChannel.trySend(TimelineEffect.OpenViewer(id))
        } else {
            selectionRepository.toggle(id)
        }
    }

    private suspend fun sendTrashEffect(effect: TrashEffect?) {
        effect?.let { effectChannel.send(TimelineEffect.Trash(it)) }
    }

    private fun today(): LocalDate = LocalDate.ofInstant(Instant.ofEpochMilli(clock.nowMillis()), clock.zone())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
