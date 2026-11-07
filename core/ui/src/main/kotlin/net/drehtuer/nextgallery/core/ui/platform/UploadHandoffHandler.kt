package net.drehtuer.nextgallery.core.ui.platform

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.model.UploadHandoff
import net.drehtuer.nextgallery.core.ui.R

/**
 * Carries out an [UploadHandoff]: opens the Nextcloud app or shows why not. [onResumed] runs whenever the screen comes
 * back, for example from the Nextcloud app.
 */
@Composable
fun rememberUploadHandoffHandler(
    snackbarHostState: SnackbarHostState,
    onResumed: () -> Unit,
): (UploadHandoff) -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val currentOnResumed by rememberUpdatedState(onResumed)
    LifecycleResumeEffect(Unit) {
        currentOnResumed()
        onPauseOrDispose { }
    }
    return remember(context, resources, snackbarHostState, scope) {
        { handoff ->
            val message = when (handoff) {
                is UploadHandoff.Ready -> when (context.handOffImages(handoff.spec)) {
                    HandoffStart.STARTED -> null
                    HandoffStart.APP_MISSING -> R.string.core_ui_upload_app_missing
                    HandoffStart.FAILED -> R.string.core_ui_upload_failed
                }

                UploadHandoff.NothingToUpload -> R.string.core_ui_upload_nothing_to_do

                UploadHandoff.NextcloudAppMissing -> R.string.core_ui_upload_app_missing
            }
            message?.let { scope.launch { snackbarHostState.showSnackbar(resources.getString(it)) } }
        }
    }
}
