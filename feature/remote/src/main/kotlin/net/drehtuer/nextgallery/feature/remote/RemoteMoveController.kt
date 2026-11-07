package net.drehtuer.nextgallery.feature.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFileOperations
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteMoveFlow
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteMoveResult
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteMoveStep
import net.drehtuer.nextgallery.core.model.RemoteItem

/** The move sheet, the conflict dialog and undo (D46). The decisions are in [RemoteMoveFlow]. */
internal class RemoteMoveController(
    private val scope: CoroutineScope,
    private val operations: RemoteFileOperations,
    private val flow: RemoteMoveFlow,
    private val sendEffect: (RemoteBrowserEffect) -> Unit,
    private val onServerChanged: () -> Unit,
) {
    private val mutableState = MutableStateFlow(RemoteMoveUiState())
    private var target = ""

    val state: StateFlow<RemoteMoveUiState> = mutableState

    fun open(items: List<RemoteItem>, startPath: String) {
        mutableState.update { it.copy(sheet = MoveSheetState(items, startPath)) }
        loadSheet(startPath)
    }

    fun onEvent(event: MoveEvent) {
        when (event) {
            is MoveEvent.SheetFolderOpened -> loadSheet(event.path)
            MoveEvent.SheetDismissed -> mutableState.update { it.copy(sheet = null) }
            MoveEvent.NewFolderRequested -> updateSheet { it.copy(showNewFolder = true) }
            MoveEvent.NewFolderDismissed -> updateSheet { it.copy(showNewFolder = false) }
            is MoveEvent.NewFolderConfirmed -> createFolder(event.name)
            MoveEvent.MoveHere -> moveHere()
            is MoveEvent.ConflictResolved -> runStep { flow.resolve(event.policy, event.applyToAll) }
            MoveEvent.Undo -> undo()
        }
    }

    private fun updateSheet(transform: (MoveSheetState) -> MoveSheetState) {
        mutableState.update { state -> state.copy(sheet = state.sheet?.let(transform)) }
    }

    private fun loadSheet(path: String) {
        updateSheet { it.copy(path = path, folders = emptyList(), isLoading = true, error = null) }
        scope.launch {
            val result = operations.listFolder(path)
            updateSheet { sheet ->
                if (sheet.path != path) return@updateSheet sheet
                when (result) {
                    is AppResult.Success -> sheet.copy(
                        folders = result.value.folders.map { MoveFolder(it, operations.canWrite(it)) },
                        canWriteHere = operations.canWrite(result.value.self),
                        isLoading = false,
                    )

                    is AppResult.Failure -> sheet.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    private fun createFolder(name: String) {
        val parent = mutableState.value.sheet?.path ?: return
        updateSheet { it.copy(showNewFolder = false) }
        scope.launch {
            when (val result = operations.createFolder(parent, name)) {
                is AppResult.Success -> loadSheet(result.value)
                is AppResult.Failure -> sendEffect(RemoteBrowserEffect.Failed(result.error))
            }
        }
    }

    private fun moveHere() {
        val sheet = mutableState.value.sheet?.takeIf { it.canMoveHere } ?: return
        target = sheet.path
        runStep { flow.start(sheet.items, sheet.path) }
    }

    /** Runs one step of the move; the next step is either a question to the user or the end. */
    private fun runStep(step: suspend () -> RemoteMoveStep) {
        mutableState.update { it.copy(sheet = null, conflict = null, isMoving = true) }
        scope.launch {
            when (val next = step()) {
                is RemoteMoveStep.Conflict -> mutableState.update {
                    it.copy(
                        isMoving = false,
                        conflict = MoveConflictState(next.item, next.existing, target, next.remaining),
                    )
                }

                is RemoteMoveStep.Finished -> finish(next.results)
            }
        }
    }

    private fun finish(results: List<RemoteMoveResult>) {
        mutableState.update { it.copy(isMoving = false) }
        val moved = results.count {
            it is RemoteMoveResult.Moved || it is RemoteMoveResult.Renamed || it is RemoteMoveResult.Replaced
        }
        if (moved > 0) sendEffect(RemoteBrowserEffect.Moved(moved, target))
        results.filterIsInstance<RemoteMoveResult.Failed>().firstOrNull()?.let { failure ->
            sendEffect(RemoteBrowserEffect.Failed(failure.error))
        }
        onServerChanged()
    }

    private fun undo() {
        scope.launch {
            val result = flow.undo()
            sendFirstFailure(result.failures.map { it.second })
            onServerChanged()
        }
    }

    private fun sendFirstFailure(errors: List<AppError>) {
        errors.firstOrNull()?.let { sendEffect(RemoteBrowserEffect.Failed(it)) }
    }
}
