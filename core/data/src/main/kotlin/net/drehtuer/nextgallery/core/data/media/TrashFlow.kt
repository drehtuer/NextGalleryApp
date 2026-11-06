package net.drehtuer.nextgallery.core.data.media

import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.TrashEffect
import javax.inject.Inject

/**
 * The steps of moving images to the system trash, shared by all screens with a delete action: request,
 * system confirmation, then the option to undo. Each ViewModel has its own instance.
 */
class TrashFlow @Inject constructor(private val actions: MediaActionsRepository) {
    private var pendingTrash: DeleteOutcome.NeedsUserConfirmation? = null
    private var pendingIds: Collection<MediaId> = emptyList()
    private var trashedUris: List<String> = emptyList()

    suspend fun request(ids: Collection<MediaId>): TrashEffect? = when (val outcome = actions.delete(ids)) {
        is DeleteOutcome.NeedsUserConfirmation -> {
            pendingTrash = outcome
            pendingIds = ids
            TrashEffect.RequestConfirmation(outcome.confirmation)
        }

        DeleteOutcome.NothingToDelete -> null

        is DeleteOutcome.Failed -> TrashEffect.Failed
    }

    /** Also receives the result of an undo, which needs no further step: the next sync finds the images again. */
    suspend fun onConfirmationResult(confirmed: Boolean): TrashEffect? {
        val trash = pendingTrash
        pendingTrash = null
        if (trash == null || !confirmed) return null
        actions.onDeleteConfirmed(pendingIds)
        trashedUris = trash.contentUris
        return TrashEffect.MovedToTrash(pendingIds.size)
    }

    suspend fun undo(): TrashEffect? {
        val uris = trashedUris.takeIf { it.isNotEmpty() } ?: return null
        trashedUris = emptyList()
        return when (val request = actions.restore(uris)) {
            is AppResult.Success -> TrashEffect.RequestConfirmation(request.value)
            is AppResult.Failure -> TrashEffect.Failed
        }
    }
}
