package net.drehtuer.nextgallery.feature.editor

import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.data.edit.EditGeometry
import net.drehtuer.nextgallery.core.data.edit.EditRepository
import net.drehtuer.nextgallery.core.data.edit.MemoryBudget
import net.drehtuer.nextgallery.core.data.edit.SaveMode
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import net.drehtuer.nextgallery.core.model.edit.AspectRatio
import net.drehtuer.nextgallery.core.model.edit.CropHandle
import net.drehtuer.nextgallery.core.model.edit.CropMath
import net.drehtuer.nextgallery.core.model.edit.EditHistory
import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.NormalizedDistance
import net.drehtuer.nextgallery.core.model.edit.NormalizedPoint
import net.drehtuer.nextgallery.core.model.edit.NormalizedRect
import net.drehtuer.nextgallery.core.model.edit.PreviewImage

/** The tools that stay active. Rotate and Scale are single actions (DESIGN.md). */
enum class EditorTool { CROP, DRAW }

sealed interface EditorMessage {
    /** D30; the values are in megabytes. */
    data class NotEnoughMemory(val requiredMegabytes: Long, val availableMegabytes: Long) : EditorMessage

    data object SaveFailed : EditorMessage

    data object PreviewFailed : EditorMessage

    companion object {
        fun notEnoughMemory(requiredBytes: Long, availableBytes: Long) = NotEnoughMemory(
            MemoryBudget.requiredMegabytes(requiredBytes),
            MemoryBudget.availableMegabytes(availableBytes),
        )

        fun from(error: AppError, otherwise: EditorMessage): EditorMessage = when (error) {
            is AppError.InsufficientMemory -> notEnoughMemory(error.requiredBytes, error.availableBytes)
            else -> otherwise
        }
    }
}

/**
 * [sourceSize] and [imageSize] are the full upright size of the file and the size the edit has at full resolution.
 * [cropRect] is the crop being set up, relative to the current image; leaving the Crop tool or saving applies it.
 */
data class EditorUiState(
    val displayName: String = "",
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val preview: PreviewImage? = null,
    val sourceSize: ImageSize? = null,
    val imageSize: ImageSize? = null,
    val history: EditHistory = EditHistory(),
    val tool: EditorTool? = null,
    val cropRect: NormalizedRect = NormalizedRect.FULL,
    val aspect: AspectRatio = AspectRatio.FREE,
    val penColor: Int = DEFAULT_PEN_COLOR,
    val penWidth: Int = DEFAULT_PEN_WIDTH,
    val isScaleDialogShown: Boolean = false,
    val isDiscardDialogShown: Boolean = false,
    val isSaving: Boolean = false,
    val message: EditorMessage? = null,
) {
    private val hasPendingCrop: Boolean get() = cropRect != NormalizedRect.FULL

    val canUndo: Boolean get() = history.canUndo || hasPendingCrop

    val canRedo: Boolean get() = history.canRedo

    val hasChanges: Boolean get() = canUndo

    /** The pen width is given in pixels of the preview, so it looks the same on screen at every resolution. */
    val penWidthFraction: Float
        get() = penWidth.toFloat() / (preview?.let { maxOf(it.width, it.height) } ?: DEFAULT_PREVIEW_SIDE)

    companion object {
        const val DEFAULT_PEN_COLOR = -1 // White
        const val DEFAULT_PEN_WIDTH = 8
        private const val DEFAULT_PREVIEW_SIDE = EditRepository.PREVIEW_MAX_SIDE
    }
}

sealed interface EditorEvent {
    data object Rotate : EditorEvent

    /** Selecting the active tool again closes it. */
    data class SelectTool(val tool: EditorTool) : EditorEvent

    data class CropDragged(val handle: CropHandle, val distance: NormalizedDistance) : EditorEvent

    data class AspectChanged(val aspect: AspectRatio) : EditorEvent

    data object ShowScaleDialog : EditorEvent

    data object HideScaleDialog : EditorEvent

    data class ApplyScale(val target: ImageSize) : EditorEvent

    data class PenColorChanged(val colorArgb: Int) : EditorEvent

    data class PenWidthChanged(val width: Int) : EditorEvent

    data class StrokeDrawn(val points: List<NormalizedPoint>) : EditorEvent

    data object Undo : EditorEvent

    data object Redo : EditorEvent

    data class Save(val mode: SaveMode) : EditorEvent

    data class ConfirmationResult(val confirmed: Boolean) : EditorEvent

    data object Close : EditorEvent

    data object KeepEditing : EditorEvent

    data object Discard : EditorEvent

    data object DismissMessage : EditorEvent
}

sealed interface EditorEffect {
    data class RequestConfirmation(val confirmation: PlatformConfirmation) : EditorEffect

    data class OpenCopy(val id: MediaId, val bucketId: Long) : EditorEffect

    /** The image changed under its own URI. */
    data object Overwritten : EditorEffect

    data object Close : EditorEffect
}

/** The changes of the editor state that need nothing but the state. The ViewModel renders if the history changed. */
internal object EditorReducer {
    fun reduce(state: EditorUiState, event: EditorEvent): EditorUiState = when (event) {
        EditorEvent.Rotate -> state.commitCrop().push(EditOperation.Rotate(1))
        is EditorEvent.SelectTool -> state.commitCrop().copy(tool = event.tool.takeUnless { it == state.tool })
        is EditorEvent.CropDragged -> dragCrop(state, event)
        is EditorEvent.AspectChanged -> changeAspect(state, event.aspect)
        is EditorEvent.StrokeDrawn -> drawStroke(state, event.points)
        EditorEvent.ShowScaleDialog -> state.commitCrop().copy(isScaleDialogShown = true)
        EditorEvent.Undo -> undo(state)
        EditorEvent.Redo -> if (state.canRedo) state.withHistory(state.history.redo()) else state
        else -> reduceSettings(state, event)
    }

    /** Applies a scale that passed the memory check. */
    fun applyScale(state: EditorUiState, target: ImageSize): EditorUiState =
        state.commitCrop().push(EditOperation.Scale(target.width, target.height))

    /** Returns the state with a crop being set up turned into an operation, as a save needs it. */
    fun EditorUiState.commitCrop(): EditorUiState = if (cropRect == NormalizedRect.FULL) {
        copy(aspect = AspectRatio.FREE)
    } else {
        withHistory(history.push(EditOperation.Crop(cropRect)))
    }

    private fun reduceSettings(state: EditorUiState, event: EditorEvent): EditorUiState = when (event) {
        EditorEvent.HideScaleDialog -> state.copy(isScaleDialogShown = false)
        is EditorEvent.PenColorChanged -> state.copy(penColor = event.colorArgb)
        is EditorEvent.PenWidthChanged -> state.copy(penWidth = event.width)
        EditorEvent.KeepEditing -> state.copy(isDiscardDialogShown = false)
        EditorEvent.DismissMessage -> state.copy(message = null)
        else -> state
    }

    private fun EditorUiState.push(operation: EditOperation) = withHistory(history.push(operation))

    private fun EditorUiState.withHistory(newHistory: EditHistory) = copy(
        history = newHistory,
        imageSize = sourceSize?.let { EditGeometry.outputSize(newHistory.operations, it) },
        cropRect = NormalizedRect.FULL,
        aspect = AspectRatio.FREE,
    )

    private fun dragCrop(state: EditorUiState, event: EditorEvent.CropDragged): EditorUiState {
        val size = state.imageSize ?: return state
        return state.copy(cropRect = CropMath.drag(state.cropRect, event.handle, event.distance, state.aspect, size))
    }

    /** A ratio takes the largest frame that fits; Free leaves the frame as it is. */
    private fun changeAspect(state: EditorUiState, aspect: AspectRatio): EditorUiState {
        val size = state.imageSize ?: return state
        val rect = if (aspect == AspectRatio.FREE) state.cropRect else NormalizedRect.FULL
        return state.copy(aspect = aspect, cropRect = CropMath.constrain(rect, aspect, size))
    }

    private fun drawStroke(state: EditorUiState, points: List<NormalizedPoint>): EditorUiState = if (points.isEmpty()) {
        state
    } else {
        state.commitCrop().push(EditOperation.PenStroke(points, state.penColor, state.penWidthFraction))
    }

    /** A crop being set up counts as the last change, so it is the first to go. */
    private fun undo(state: EditorUiState): EditorUiState = when {
        state.cropRect != NormalizedRect.FULL -> state.copy(cropRect = NormalizedRect.FULL)
        state.history.canUndo -> state.withHistory(state.history.undo())
        else -> state
    }
}
