package net.drehtuer.nextgallery.core.data.media

import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import net.drehtuer.nextgallery.core.model.ShareSpec
import javax.inject.Inject

sealed interface DeleteOutcome {
    data class NeedsUserConfirmation(val confirmation: PlatformConfirmation, val contentUris: List<String>) :
        DeleteOutcome

    data object NothingToDelete : DeleteOutcome

    data class Failed(val error: AppError) : DeleteOutcome
}

interface MediaActionsRepository {
    /** Moves images to the system trash, or deletes them for good if [permanent] is set (D10). */
    suspend fun delete(ids: Collection<MediaId>, permanent: Boolean = false): DeleteOutcome

    /** The user confirmed: updates the index and the selection right away, without waiting for the next sync. */
    suspend fun onDeleteConfirmed(ids: Collection<MediaId>)

    /** Takes images out of the system trash. The next sync finds them again. */
    suspend fun restore(contentUris: List<String>): AppResult<PlatformConfirmation>

    suspend fun shareSpec(ids: Collection<MediaId>): ShareSpec?
}

class DefaultMediaActionsRepository @Inject constructor(
    private val indexStore: MediaIndexStore,
    private val deleteSource: MediaDeleteSource,
    private val selectionRepository: SelectionRepository,
) : MediaActionsRepository {
    override suspend fun delete(ids: Collection<MediaId>, permanent: Boolean): DeleteOutcome {
        val uris = indexStore.getByIds(ids).map { it.contentUri }
        if (uris.isEmpty()) return DeleteOutcome.NothingToDelete
        val request = runCatchingCancellable {
            if (permanent) deleteSource.deleteRequest(uris) else deleteSource.trashRequest(uris)
        }
        return when (request) {
            is AppResult.Success -> DeleteOutcome.NeedsUserConfirmation(request.value, uris)
            is AppResult.Failure -> DeleteOutcome.Failed(request.error)
        }
    }

    override suspend fun onDeleteConfirmed(ids: Collection<MediaId>) {
        indexStore.deleteByIds(ids.map { it.value })
        selectionRepository.deselect(ids)
    }

    override suspend fun restore(contentUris: List<String>): AppResult<PlatformConfirmation> =
        runCatchingCancellable { deleteSource.restoreRequest(contentUris) }

    override suspend fun shareSpec(ids: Collection<MediaId>): ShareSpec? = ShareSpec.from(indexStore.getByIds(ids))
}
