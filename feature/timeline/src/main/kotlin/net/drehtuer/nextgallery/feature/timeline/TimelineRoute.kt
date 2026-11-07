package net.drehtuer.nextgallery.feature.timeline

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.serialization.Serializable
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.ui.platform.rememberTrashEffectHandler
import net.drehtuer.nextgallery.core.ui.platform.rememberUploadHandoffHandler
import net.drehtuer.nextgallery.core.ui.platform.shareImages

@Serializable
data object TimelineRoute

fun NavGraphBuilder.timelineScreen(onOpenViewer: (id: MediaId) -> Unit, onOpenSelectionOverview: () -> Unit) {
    composable<TimelineRoute> {
        val viewModel = hiltViewModel<TimelineViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val handleTrashEffect = rememberTrashEffectHandler(
            snackbarHostState = snackbarHostState,
            onConfirmationResult = { viewModel.onEvent(TimelineEvent.ConfirmationResult(it)) },
            onUndo = { viewModel.onEvent(TimelineEvent.UndoDelete) },
        )
        val handleUpload = rememberUploadHandoffHandler(snackbarHostState) { viewModel.onEvent(TimelineEvent.Resumed) }
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is TimelineEffect.OpenViewer -> onOpenViewer(effect.id)
                    is TimelineEffect.Share -> context.shareImages(effect.spec)
                    is TimelineEffect.Upload -> handleUpload(effect.handoff)
                    is TimelineEffect.Trash -> handleTrashEffect(effect.effect)
                }
            }
        }
        TimelineScreen(
            state = state,
            items = viewModel.items.collectAsLazyPagingItems(),
            onEvent = viewModel::onEvent,
            onOpenSelectionOverview = onOpenSelectionOverview,
            snackbarHostState = snackbarHostState,
        )
    }
}
