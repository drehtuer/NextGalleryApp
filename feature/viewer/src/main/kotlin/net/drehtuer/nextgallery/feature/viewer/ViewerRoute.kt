package net.drehtuer.nextgallery.feature.viewer

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.serialization.Serializable
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.ui.platform.rememberTrashEffectHandler
import net.drehtuer.nextgallery.core.ui.platform.rememberUploadHandoffHandler
import net.drehtuer.nextgallery.core.ui.platform.shareImages

/** Pages through the map cluster [mapBounds] ([GeoBounds.encode]) if set, the folder [bucketId], or the timeline. */
@Serializable
data class ViewerRoute(val mediaId: Long, val bucketId: Long?, val mapBounds: String? = null) {
    internal fun source(): MediaSource {
        val bounds = mapBounds?.let(GeoBounds::decode)
        return when {
            bounds != null -> MediaSource.Map(bounds)
            bucketId != null -> MediaSource.Folder(bucketId)
            else -> MediaSource.Timeline
        }
    }
}

fun NavGraphBuilder.viewerScreen(onBack: () -> Unit, onOpenEditor: (MediaId) -> Unit) {
    composable<ViewerRoute> { entry ->
        val route = entry.toRoute<ViewerRoute>()
        val viewModel = hiltViewModel<ViewerViewModel, ViewerViewModel.Factory>(
            creationCallback = { factory -> factory.create(route) },
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val handleTrashEffect = rememberTrashEffectHandler(
            snackbarHostState = snackbarHostState,
            onConfirmationResult = { viewModel.onEvent(ViewerEvent.ConfirmationResult(it)) },
            onUndo = { viewModel.onEvent(ViewerEvent.UndoDelete) },
        )
        val handleUpload = rememberUploadHandoffHandler(snackbarHostState) { viewModel.onEvent(ViewerEvent.Resumed) }
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ViewerEffect.Share -> context.shareImages(effect.spec)
                    is ViewerEffect.Upload -> handleUpload(effect.handoff)
                    is ViewerEffect.Trash -> handleTrashEffect(effect.effect)
                    is ViewerEffect.OpenEditor -> onOpenEditor(effect.id)
                    ViewerEffect.Close -> onBack()
                }
            }
        }
        ViewerScreen(
            state = state,
            items = viewModel.items.collectAsLazyPagingItems(),
            onEvent = viewModel::onEvent,
            onBack = onBack,
            snackbarHostState = snackbarHostState,
        )
    }
}
