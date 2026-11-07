package net.drehtuer.nextgallery.feature.map

import kotlinx.coroutines.flow.StateFlow
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.MediaId
import javax.inject.Inject

/** The map's part of the app-wide selection (D33). */
class MapSelectionActions @Inject constructor(
    private val selectionRepository: SelectionRepository,
    private val actions: MediaActionsRepository,
    private val trashFlow: TrashFlow,
) {
    fun observe(): StateFlow<Set<MediaId>> = selectionRepository.observe()

    /** [openBounds] belongs to the open cluster. Returns what the screen has to do next, if anything. */
    suspend fun handle(event: MapSelectionEvent, openBounds: GeoBounds?): MapEffect? {
        val selected = selectionRepository.observe().value
        return when (event) {
            is MapSelectionEvent.ItemClicked -> {
                // During a selection a click changes the selection; otherwise it opens the image.
                if (openBounds != null && selected.isEmpty()) return MapEffect.OpenViewer(event.id, openBounds)
                selectionRepository.toggle(event.id)
                null
            }

            is MapSelectionEvent.ItemLongPressed -> {
                selectionRepository.toggle(event.id)
                null
            }

            MapSelectionEvent.Clear -> {
                selectionRepository.clear()
                null
            }

            MapSelectionEvent.Share -> actions.shareSpec(selected)?.let { MapEffect.Share(it) }

            MapSelectionEvent.Delete -> trashFlow.request(selected)?.let { MapEffect.Trash(it) }

            is MapSelectionEvent.ConfirmationResult ->
                trashFlow.onConfirmationResult(event.confirmed)?.let { MapEffect.Trash(it) }

            MapSelectionEvent.UndoDelete -> trashFlow.undo()?.let { MapEffect.Trash(it) }
        }
    }
}
