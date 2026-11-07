package net.drehtuer.nextgallery.feature.remote

import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.data.nextcloud.ConflictPolicy
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.model.ShareSpec

/** The long-press menu (D47): opened at the image [anchorPath], for [items]. */
data class RemoteMenuState(val anchorPath: String, val items: List<RemoteItem>)

/** A folder in the move sheet; [canWrite] is false for read-only shares (D46). */
data class MoveFolder(val item: RemoteItem, val canWrite: Boolean)

data class MoveSheetState(
    val items: List<RemoteItem>,
    val path: String,
    val folders: List<MoveFolder> = emptyList(),
    val isLoading: Boolean = true,
    val canWriteHere: Boolean = true,
    val error: AppError? = null,
    val showNewFolder: Boolean = false,
) {
    /** The images are all in the folder shown, so moving them here would do nothing. */
    val isSourceFolder: Boolean get() = items.all { it.parentPath == path }

    val canMoveHere: Boolean get() = !isLoading && canWriteHere && !isSourceFolder && error == null
}

data class MoveConflictState(
    val item: RemoteItem,
    val existing: RemoteItem?,
    val targetFolder: String,
    val otherImages: Int,
)

data class RemoteMoveUiState(
    val sheet: MoveSheetState? = null,
    val conflict: MoveConflictState? = null,
    val isMoving: Boolean = false,
)

data class RemoteShareUiState(val current: Int, val total: Int)

/** The server's trash bin is off or unknown, so deleting [items] needs a confirmation (D56). */
data class DeleteConfirmationState(val items: List<RemoteItem>)

data class RemoteBrowserUiState(
    val loginRequired: Boolean = false,
    val path: String = RemotePaths.ROOT,
    val folders: List<RemoteItem> = emptyList(),
    val images: List<RemoteItem> = emptyList(),
    val urls: RemoteImageUrls? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isConnected: Boolean = true,
    val serverUnreachable: Boolean = false,
    val sessionEnded: Boolean = false,
    val error: AppError? = null,
    val certificate: CertificateInfo? = null,
    val selection: Map<String, RemoteItem> = emptyMap(),
    val selecting: Boolean = false,
    val menu: RemoteMenuState? = null,
    val move: RemoteMoveUiState = RemoteMoveUiState(),
    val share: RemoteShareUiState? = null,
    val deleteConfirmation: DeleteConfirmationState? = null,
) {
    val isOffline: Boolean get() = !isConnected || serverUnreachable
}

sealed interface RemoteBrowserEvent

sealed interface BrowseEvent : RemoteBrowserEvent {
    data class OpenFolder(val path: String) : BrowseEvent

    data object Up : BrowseEvent

    data object Refresh : BrowseEvent

    data object TrustCertificate : BrowseEvent

    data object RejectCertificate : BrowseEvent
}

sealed interface SelectionEvent : RemoteBrowserEvent {
    data class ItemClicked(val item: RemoteItem) : SelectionEvent

    data class ItemLongPressed(val item: RemoteItem) : SelectionEvent

    data object StartSelecting : SelectionEvent

    data object SelectAll : SelectionEvent

    data object ClearSelection : SelectionEvent

    data object MenuDismissed : SelectionEvent

    data object MenuMove : SelectionEvent

    data object MenuShare : SelectionEvent

    data object MenuDelete : SelectionEvent

    data object MoveSelected : SelectionEvent
}

sealed interface MoveEvent : RemoteBrowserEvent {
    data class SheetFolderOpened(val path: String) : MoveEvent

    data object SheetDismissed : MoveEvent

    data object NewFolderRequested : MoveEvent

    data object NewFolderDismissed : MoveEvent

    data class NewFolderConfirmed(val name: String) : MoveEvent

    data object MoveHere : MoveEvent

    data class ConflictResolved(val policy: ConflictPolicy, val applyToAll: Boolean) : MoveEvent

    data object Undo : MoveEvent
}

sealed interface ActionEvent : RemoteBrowserEvent {
    data object UndoDelete : ActionEvent

    data object ConfirmDelete : ActionEvent

    data object CancelDelete : ActionEvent

    data object CancelShare : ActionEvent
}

/** What needs a connection and was asked for without one (D46). */
enum class RemoteOfflineAction { MOVE, SHARE, DELETE }

sealed interface RemoteBrowserEffect {
    data class OpenViewer(val path: String) : RemoteBrowserEffect

    data class Share(val spec: ShareSpec) : RemoteBrowserEffect

    /** [targetFolder] is the folder's path; offers Undo. */
    data class Moved(val count: Int, val targetFolder: String) : RemoteBrowserEffect

    /** Into the trash bin; offers Undo. */
    data class Deleted(val count: Int) : RemoteBrowserEffect

    /** Without a trash bin (D56), so without Undo. */
    data class DeletedPermanently(val count: Int) : RemoteBrowserEffect

    data class Failed(val error: AppError) : RemoteBrowserEffect

    data class Offline(val action: RemoteOfflineAction) : RemoteBrowserEffect
}
