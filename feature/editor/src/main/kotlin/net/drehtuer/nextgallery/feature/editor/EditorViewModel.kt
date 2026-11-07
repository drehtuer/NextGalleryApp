package net.drehtuer.nextgallery.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.edit.EditRepository
import net.drehtuer.nextgallery.core.data.edit.EditSource
import net.drehtuer.nextgallery.core.data.edit.MemoryCheck
import net.drehtuer.nextgallery.core.data.edit.SaveMode
import net.drehtuer.nextgallery.core.data.edit.SaveOutcome
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.feature.editor.EditorReducer.commitCrop

@HiltViewModel(assistedFactory = EditorViewModel.Factory::class)
class EditorViewModel @AssistedInject constructor(
    @Assisted route: EditorRoute,
    private val repository: EditRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(route: EditorRoute): EditorViewModel
    }

    private val mediaId = MediaId(route.mediaId)
    private val mutableState = MutableStateFlow(EditorUiState())
    private val effectChannel = Channel<EditorEffect>(Channel.BUFFERED)
    private var source: EditSource? = null
    private var renderJob: Job? = null

    val state: StateFlow<EditorUiState> = mutableState.asStateFlow()

    val effects: Flow<EditorEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.open(mediaId)) {
                is AppResult.Success -> onOpened(result.value)
                is AppResult.Failure -> mutableState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    /** While an image is being saved, only the answer to the system's question counts. */
    fun onEvent(event: EditorEvent) {
        when {
            event is EditorEvent.ConfirmationResult -> onConfirmationResult(event.confirmed)
            !mutableState.value.isSaving -> handle(event)
        }
    }

    private fun handle(event: EditorEvent) {
        when (event) {
            is EditorEvent.ApplyScale -> applyScale(event.target)
            is EditorEvent.Save -> save(event.mode)
            EditorEvent.Close -> close()
            EditorEvent.Discard -> effectChannel.trySend(EditorEffect.Close)
            else -> setState(EditorReducer.reduce(mutableState.value, event))
        }
    }

    private fun onOpened(opened: EditSource) {
        source = opened
        mutableState.update {
            it.copy(
                displayName = opened.item.displayName,
                isLoading = false,
                preview = opened.base,
                sourceSize = opened.size,
                imageSize = opened.size,
            )
        }
    }

    /** A change of the operations also needs a new preview. */
    private fun setState(newState: EditorUiState) {
        val changed = newState.history != mutableState.value.history
        mutableState.value = newState
        if (changed) renderPreview(newState.history.operations)
    }

    private fun renderPreview(operations: List<EditOperation>) {
        val opened = source ?: return
        renderJob?.cancel()
        if (operations.isEmpty()) {
            mutableState.update { it.copy(preview = opened.base) }
            return
        }
        renderJob = viewModelScope.launch {
            when (val result = repository.renderPreview(opened, operations)) {
                is AppResult.Success -> mutableState.update { it.copy(preview = result.value) }

                is AppResult.Failure -> mutableState.update {
                    it.copy(message = EditorMessage.from(result.error, EditorMessage.PreviewFailed))
                }
            }
        }
    }

    /** A refused scale leaves the image as it is and tells why (D30). */
    private fun applyScale(target: ImageSize) {
        val opened = source ?: return
        val hidden = mutableState.value.copy(isScaleDialogShown = false)
        val operations = hidden.history.operations + EditOperation.Scale(target.width, target.height)
        when (val check = repository.checkMemory(opened, operations)) {
            MemoryCheck.Fits -> setState(EditorReducer.applyScale(hidden, target))

            is MemoryCheck.Refused -> mutableState.value = hidden.copy(
                message = EditorMessage.notEnoughMemory(check.requiredBytes, check.availableBytes),
            )
        }
    }

    private fun save(mode: SaveMode) {
        setState(mutableState.value.commitCrop())
        val operations = mutableState.value.history.operations
        mutableState.update { it.copy(isSaving = true) }
        viewModelScope.launch { onSaveOutcome(repository.save(mediaId, operations, mode)) }
    }

    private fun onConfirmationResult(confirmed: Boolean) {
        if (!mutableState.value.isSaving) return
        if (confirmed) {
            val operations = mutableState.value.history.operations
            viewModelScope.launch { onSaveOutcome(repository.commitOverwrite(mediaId, operations)) }
        } else {
            mutableState.update { it.copy(isSaving = false) }
        }
    }

    private suspend fun onSaveOutcome(outcome: SaveOutcome) {
        when (outcome) {
            is SaveOutcome.SavedCopy -> effectChannel.send(
                if (outcome.indexed) EditorEffect.OpenCopy(outcome.id, outcome.bucketId) else EditorEffect.Close,
            )

            SaveOutcome.Overwritten -> effectChannel.send(EditorEffect.Overwritten)

            is SaveOutcome.NeedsConfirmation -> effectChannel.send(
                EditorEffect.RequestConfirmation(outcome.confirmation),
            )

            is SaveOutcome.Failed -> mutableState.update {
                it.copy(isSaving = false, message = EditorMessage.from(outcome.error, EditorMessage.SaveFailed))
            }
        }
    }

    private fun close() {
        if (mutableState.value.hasChanges) {
            mutableState.update { it.copy(isDiscardDialogShown = true) }
        } else {
            effectChannel.trySend(EditorEffect.Close)
        }
    }
}
