package net.drehtuer.nextgallery.feature.folders

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.serialization.Serializable
import net.drehtuer.nextgallery.core.model.MediaId

@Serializable
data object FolderListRoute

@Serializable
data class FolderGridRoute(val bucketId: Long)

fun NavGraphBuilder.folderListScreen(onFolderClick: (bucketId: Long) -> Unit) {
    composable<FolderListRoute> {
        val viewModel = hiltViewModel<FolderListViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        FolderListScreen(state, onFolderClick)
    }
}

fun NavGraphBuilder.folderGridScreen(onBack: () -> Unit, onOpenViewer: (MediaId) -> Unit) {
    composable<FolderGridRoute> { entry ->
        val route = entry.toRoute<FolderGridRoute>()
        val viewModel = hiltViewModel<FolderGridViewModel, FolderGridViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.bucketId) },
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is FolderGridEffect.OpenViewer -> onOpenViewer(effect.id)
                }
            }
        }
        FolderGridScreen(state, viewModel.items.collectAsLazyPagingItems(), viewModel::onEvent, onBack)
    }
}
