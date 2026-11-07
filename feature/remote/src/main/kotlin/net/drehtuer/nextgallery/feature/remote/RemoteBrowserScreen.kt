package net.drehtuer.nextgallery.feature.remote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.ui.component.BackButton
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.ErrorState
import net.drehtuer.nextgallery.core.ui.component.MediaGridPlaceholder
import net.drehtuer.nextgallery.core.ui.component.SelectionTopBar
import net.drehtuer.nextgallery.core.ui.remote.Breadcrumbs
import net.drehtuer.nextgallery.core.ui.remote.CertificateWarningDialog
import net.drehtuer.nextgallery.core.ui.remote.remoteMessage

/** The Nextcloud tab: login hint while logged out (D35), otherwise the folders and images of the server. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteBrowserScreen(
    state: RemoteBrowserUiState,
    onEvent: (RemoteBrowserEvent) -> Unit,
    onSignIn: (signInAgain: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    if (state.loginRequired) {
        LoginRequired(onSignIn = { onSignIn(false) }, modifier)
        return
    }
    BackHandler(enabled = state.selecting) { onEvent(SelectionEvent.ClearSelection) }
    BackHandler(enabled = !state.selecting && state.path != RemotePaths.ROOT) { onEvent(BrowseEvent.Up) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BrowserTopBar(state, onEvent) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isOffline) OfflineBanner()
            if (state.sessionEnded) SessionEndedBanner(onSignIn = { onSignIn(true) })
            if (state.move.isMoving) LinearProgressIndicator(Modifier.fillMaxWidth())
            Breadcrumbs(state.path, onOpen = { onEvent(BrowseEvent.OpenFolder(it)) })
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(BrowseEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                BrowserContent(state, onEvent)
            }
        }
    }
    BrowserDialogs(state, onEvent)
}

@Composable
private fun BrowserContent(state: RemoteBrowserUiState, onEvent: (RemoteBrowserEvent) -> Unit) {
    val hasItems = state.folders.isNotEmpty() || state.images.isNotEmpty()
    val error = state.error
    when {
        hasItems -> RemoteGrid(state, onEvent)

        state.isLoading -> MediaGridPlaceholder()

        error != null -> ErrorState(
            title = stringResource(R.string.feature_remote_load_failed),
            message = stringResource(error.remoteMessage()),
            onRetry = { onEvent(BrowseEvent.Refresh) },
        )

        else -> EmptyState(
            title = stringResource(R.string.feature_remote_empty),
            detail = stringResource(R.string.feature_remote_empty_detail),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserTopBar(state: RemoteBrowserUiState, onEvent: (RemoteBrowserEvent) -> Unit) {
    if (state.selecting) {
        SelectionTopBar(
            selectionCount = state.selection.size,
            onClear = { onEvent(SelectionEvent.ClearSelection) },
            actions = {
                TopBarAction(R.drawable.feature_remote_ic_select_all, R.string.feature_remote_select_all) {
                    onEvent(SelectionEvent.SelectAll)
                }
                if (state.selection.isNotEmpty()) {
                    TopBarAction(R.drawable.feature_remote_ic_move, R.string.feature_remote_move) {
                        onEvent(SelectionEvent.MoveSelected)
                    }
                }
            },
        )
    } else {
        TopAppBar(
            title = { Text(RemotePaths.name(state.path).ifEmpty { stringResource(R.string.feature_remote_title) }) },
            navigationIcon = {
                if (state.path != RemotePaths.ROOT) BackButton(onClick = { onEvent(BrowseEvent.Up) })
            },
            actions = {
                TopBarAction(R.drawable.feature_remote_ic_select, R.string.feature_remote_select) {
                    onEvent(SelectionEvent.StartSelecting)
                }
                TopBarAction(R.drawable.feature_remote_ic_refresh, R.string.feature_remote_refresh) {
                    onEvent(BrowseEvent.Refresh)
                }
            },
        )
    }
}

@Composable
private fun TopBarAction(icon: Int, description: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = stringResource(description))
    }
}

@Composable
private fun BrowserDialogs(state: RemoteBrowserUiState, onEvent: (RemoteBrowserEvent) -> Unit) {
    state.certificate?.let { info ->
        CertificateWarningDialog(
            info = info,
            onTrust = { onEvent(BrowseEvent.TrustCertificate) },
            onReject = { onEvent(BrowseEvent.RejectCertificate) },
        )
    }
    state.move.sheet?.let { MoveSheet(it, onEvent) }
    state.move.conflict?.let { MoveConflictDialog(it, onEvent) }
    state.share?.let { ShareProgressDialog(it, onCancel = { onEvent(ActionEvent.CancelShare) }) }
}
