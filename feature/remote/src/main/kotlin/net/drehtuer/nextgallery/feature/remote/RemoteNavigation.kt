package net.drehtuer.nextgallery.feature.remote

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data object RemoteBrowserRoute

@Serializable
data class RemoteViewerRoute(val path: String)

fun NavGraphBuilder.remoteBrowserScreen(
    onOpenViewer: (path: String) -> Unit,
    onSignIn: (signInAgain: Boolean) -> Unit,
) {
    composable<RemoteBrowserRoute> {
        val viewModel = hiltViewModel<RemoteBrowserViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val activity = LocalActivity.current
        // The remote selection ends when leaving the browser (D47), but not when the phone is turned.
        DisposableEffect(viewModel) {
            onDispose {
                if (activity?.isChangingConfigurations != true) viewModel.onEvent(SelectionEvent.ClearSelection)
            }
        }
        RemoteBrowserWithEffects(state, viewModel.effects, viewModel::onEvent, onOpenViewer, onSignIn)
    }
}

fun NavGraphBuilder.remoteViewerScreen(onBack: () -> Unit) {
    composable<RemoteViewerRoute> { entry ->
        val route = entry.toRoute<RemoteViewerRoute>()
        val viewModel = hiltViewModel<RemoteViewerViewModel, RemoteViewerViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.path) },
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        RemoteViewerScreen(state, viewModel::onEvent, onBack)
    }
}
