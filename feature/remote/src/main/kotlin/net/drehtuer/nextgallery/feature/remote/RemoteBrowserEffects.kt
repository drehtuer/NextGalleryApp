package net.drehtuer.nextgallery.feature.remote

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.ui.platform.shareImages
import net.drehtuer.nextgallery.core.ui.remote.remoteMessage
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

/** The screen with its effects: messages with Undo, the share sheet and opening the viewer. */
@Composable
fun RemoteBrowserWithEffects(
    state: RemoteBrowserUiState,
    effects: Flow<RemoteBrowserEffect>,
    onEvent: (RemoteBrowserEvent) -> Unit,
    onOpenViewer: (path: String) -> Unit,
    onSignIn: (signInAgain: Boolean) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val rootName = resources.getString(CoreUiR.string.core_ui_remote_root)
    val showUndoable: (String, RemoteBrowserEvent) -> Unit = { message, undo ->
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = resources.getString(CoreUiR.string.core_ui_undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) onEvent(undo)
        }
    }
    val show: (Int) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(resources.getString(message)) }
    }
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is RemoteBrowserEffect.OpenViewer -> onOpenViewer(effect.path)

                is RemoteBrowserEffect.Share -> context.shareImages(effect.spec)

                is RemoteBrowserEffect.Moved -> showUndoable(
                    resources.getQuantityString(
                        R.plurals.feature_remote_moved,
                        effect.count,
                        effect.count,
                        RemotePaths.name(effect.targetFolder).ifEmpty { rootName },
                    ),
                    MoveEvent.Undo,
                )

                is RemoteBrowserEffect.Deleted -> showUndoable(
                    resources.getQuantityString(R.plurals.feature_remote_deleted, effect.count, effect.count),
                    ActionEvent.UndoDelete,
                )

                is RemoteBrowserEffect.Failed -> show(effect.error.remoteMessage())

                is RemoteBrowserEffect.Offline -> show(offlineMessage(effect.action))
            }
        }
    }
    RemoteBrowserScreen(state, onEvent, onSignIn, snackbarHostState = snackbarHostState)
}

internal fun offlineMessage(action: RemoteOfflineAction): Int = when (action) {
    RemoteOfflineAction.MOVE -> R.string.feature_remote_offline_move
    RemoteOfflineAction.SHARE -> R.string.feature_remote_offline_share
    RemoteOfflineAction.DELETE -> R.string.feature_remote_offline_delete
}
