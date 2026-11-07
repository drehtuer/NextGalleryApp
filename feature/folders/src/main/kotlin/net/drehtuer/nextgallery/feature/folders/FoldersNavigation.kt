package net.drehtuer.nextgallery.feature.folders

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.serialization.Serializable
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.ViewContext
import net.drehtuer.nextgallery.core.ui.platform.rememberShareHandler
import net.drehtuer.nextgallery.core.ui.platform.rememberTrashEffectHandler
import net.drehtuer.nextgallery.core.ui.platform.rememberUploadHandoffHandler

@Serializable
data object FolderListRoute

@Serializable
data class FolderGridRoute(val bucketId: Long)

/** Opened from the folder [bucketId], with [fromMap] from the map, or otherwise from the timeline. */
@Serializable
data class SelectionOverviewRoute(val bucketId: Long?, val fromMap: Boolean = false) {
    internal fun viewContext(): ViewContext = when {
        fromMap -> ViewContext.Map
        bucketId != null -> ViewContext.Folder(bucketId)
        else -> ViewContext.Timeline
    }
}

fun NavGraphBuilder.folderListScreen(onFolderClick: (bucketId: Long) -> Unit, onOpenSettings: () -> Unit) {
    composable<FolderListRoute> {
        val viewModel = hiltViewModel<FolderListViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        FolderListScreen(state, onFolderClick, onOpenSettings)
    }
}

fun NavGraphBuilder.folderGridScreen(
    onBack: () -> Unit,
    onOpenViewer: (id: MediaId, bucketId: Long) -> Unit,
    onOpenSelectionOverview: (bucketId: Long) -> Unit,
) {
    composable<FolderGridRoute> { entry ->
        val route = entry.toRoute<FolderGridRoute>()
        val viewModel = hiltViewModel<FolderGridViewModel, FolderGridViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.bucketId) },
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val handleShare = rememberShareHandler(snackbarHostState)
        val handleTrashEffect = rememberTrashEffectHandler(
            snackbarHostState = snackbarHostState,
            onConfirmationResult = { viewModel.onEvent(FolderGridEvent.ConfirmationResult(it)) },
            onUndo = { viewModel.onEvent(FolderGridEvent.UndoDelete) },
        )
        val handleUpload = rememberUploadHandoffHandler(snackbarHostState) {
            viewModel.onEvent(FolderGridEvent.Resumed)
        }
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is FolderGridEffect.OpenViewer -> onOpenViewer(effect.id, route.bucketId)
                    is FolderGridEffect.Share -> handleShare(effect.spec)
                    is FolderGridEffect.Upload -> handleUpload(effect.handoff)
                    is FolderGridEffect.Trash -> handleTrashEffect(effect.effect)
                }
            }
        }
        FolderGridScreen(
            state = state,
            items = viewModel.items.collectAsLazyPagingItems(),
            onEvent = viewModel::onEvent,
            onBack = onBack,
            onOpenSelectionOverview = { onOpenSelectionOverview(route.bucketId) },
            snackbarHostState = snackbarHostState,
        )
    }
}

fun NavGraphBuilder.selectionOverviewScreen(onBack: () -> Unit) {
    composable<SelectionOverviewRoute> { entry ->
        val route = entry.toRoute<SelectionOverviewRoute>()
        val viewModel = hiltViewModel<SelectionOverviewViewModel, SelectionOverviewViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.viewContext()) },
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val handleShare = rememberShareHandler(snackbarHostState)
        val handleTrashEffect = rememberTrashEffectHandler(
            snackbarHostState = snackbarHostState,
            onConfirmationResult = { viewModel.onEvent(SelectionOverviewEvent.ConfirmationResult(it)) },
            onUndo = { viewModel.onEvent(SelectionOverviewEvent.UndoDelete) },
        )
        val handleUpload = rememberUploadHandoffHandler(snackbarHostState) {
            viewModel.onEvent(SelectionOverviewEvent.Resumed)
        }
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is SelectionOverviewEffect.Share -> handleShare(effect.spec)
                    is SelectionOverviewEffect.Upload -> handleUpload(effect.handoff)
                    is SelectionOverviewEffect.Trash -> handleTrashEffect(effect.effect)
                }
            }
        }
        SelectionOverviewScreen(state, viewModel::onEvent, onBack, snackbarHostState = snackbarHostState)
    }
}
