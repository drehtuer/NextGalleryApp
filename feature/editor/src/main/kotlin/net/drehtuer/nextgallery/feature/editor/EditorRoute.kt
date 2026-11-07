package net.drehtuer.nextgallery.feature.editor

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.ui.platform.clearImageMemoryCache
import net.drehtuer.nextgallery.core.ui.platform.rememberConfirmationLauncher

@Serializable
data class EditorRoute(val mediaId: Long)

/** [onOpenCopy] shows a saved copy; [onClose] returns to where the editor was opened. */
fun NavGraphBuilder.editorScreen(onClose: () -> Unit, onOpenCopy: (MediaId, bucketId: Long) -> Unit) {
    composable<EditorRoute> { entry ->
        val route = entry.toRoute<EditorRoute>()
        val viewModel = hiltViewModel<EditorViewModel, EditorViewModel.Factory>(
            creationCallback = { factory -> factory.create(route) },
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val launchConfirmation = rememberConfirmationLauncher { viewModel.onEvent(EditorEvent.ConfirmationResult(it)) }
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is EditorEffect.RequestConfirmation -> launchConfirmation(effect.confirmation)

                    is EditorEffect.OpenCopy -> onOpenCopy(effect.id, effect.bucketId)

                    EditorEffect.Overwritten -> {
                        clearImageMemoryCache(context)
                        onClose()
                    }

                    EditorEffect.Close -> onClose()
                }
            }
        }
        EditorScreen(state = state, onEvent = viewModel::onEvent)
    }
}
