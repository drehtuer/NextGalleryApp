package net.drehtuer.nextgallery.feature.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteBatchResult
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFileOperations
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareProgress
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareRepository
import net.drehtuer.nextgallery.core.model.RemoteItem

/** Deleting with undo (D46) and sharing with progress (D48). */
internal class RemoteActionsController(
    private val scope: CoroutineScope,
    private val operations: RemoteFileOperations,
    private val shares: RemoteShareRepository,
    private val sendEffect: (RemoteBrowserEffect) -> Unit,
    private val onServerChanged: () -> Unit,
) {
    private val mutableShare = MutableStateFlow<RemoteShareUiState?>(null)
    private var shareJob: Job? = null
    private var lastDeleted: List<RemoteItem> = emptyList()

    val share: StateFlow<RemoteShareUiState?> = mutableShare

    fun onEvent(event: ActionEvent) {
        when (event) {
            ActionEvent.UndoDelete -> undoDelete()
            ActionEvent.CancelShare -> cancelShare()
        }
    }

    fun delete(items: List<RemoteItem>) {
        scope.launch {
            val result = operations.delete(items)
            lastDeleted = result.done
            if (result.done.isNotEmpty()) sendEffect(RemoteBrowserEffect.Deleted(result.done.size))
            sendFirstFailure(result)
            onServerChanged()
        }
    }

    private fun undoDelete() {
        val items = lastDeleted
        lastDeleted = emptyList()
        if (items.isEmpty()) return
        scope.launch {
            sendFirstFailure(operations.undoDelete(items))
            onServerChanged()
        }
    }

    fun share(items: List<RemoteItem>) {
        shareJob?.cancel()
        shareJob = scope.launch {
            shares.prepare(items).collect { progress ->
                when (progress) {
                    is RemoteShareProgress.Downloading ->
                        mutableShare.value = RemoteShareUiState(progress.current, progress.total)

                    is RemoteShareProgress.Ready -> {
                        mutableShare.value = null
                        sendEffect(RemoteBrowserEffect.Share(progress.spec))
                    }

                    is RemoteShareProgress.Failed -> {
                        mutableShare.value = null
                        sendShareFailure(progress.error)
                    }
                }
            }
        }
    }

    private fun sendShareFailure(error: AppError) {
        val effect = if (error is AppError.Network) {
            RemoteBrowserEffect.Offline(RemoteOfflineAction.SHARE)
        } else {
            RemoteBrowserEffect.Failed(error)
        }
        sendEffect(effect)
    }

    private fun cancelShare() {
        shareJob?.cancel()
        mutableShare.value = null
        shares.clear()
    }

    private fun sendFirstFailure(result: RemoteBatchResult) {
        result.failures.firstOrNull()?.let { (_, error) -> sendEffect(RemoteBrowserEffect.Failed(error)) }
    }
}
