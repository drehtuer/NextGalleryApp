package net.drehtuer.nextgallery.core.ui.platform

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.model.TrashEffect
import net.drehtuer.nextgallery.core.ui.R

/** Carries out a [TrashEffect]: shows the system confirmation or a message, with the option to undo. */
@Composable
fun rememberTrashEffectHandler(
    snackbarHostState: SnackbarHostState,
    onConfirmationResult: (confirmed: Boolean) -> Unit,
    onUndo: () -> Unit,
): (TrashEffect) -> Unit {
    val launchConfirmation = rememberConfirmationLauncher(onConfirmationResult)
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val currentOnUndo by rememberUpdatedState(onUndo)
    return remember(launchConfirmation, resources, snackbarHostState, scope) {
        { effect ->
            when (effect) {
                is TrashEffect.RequestConfirmation -> launchConfirmation(effect.confirmation)

                is TrashEffect.MovedToTrash -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getQuantityString(
                            R.plurals.core_ui_moved_to_trash,
                            effect.count,
                            effect.count,
                        ),
                        actionLabel = resources.getString(R.string.core_ui_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) currentOnUndo()
                }

                TrashEffect.Failed -> scope.launch {
                    snackbarHostState.showSnackbar(resources.getString(R.string.core_ui_trash_failed))
                }
            }
        }
    }
}
