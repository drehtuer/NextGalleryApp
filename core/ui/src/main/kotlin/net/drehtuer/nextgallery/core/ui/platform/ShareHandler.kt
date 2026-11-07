package net.drehtuer.nextgallery.core.ui.platform

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.ui.R

/** Opens the share sheet for a [ShareSpec], or says so on [snackbarHostState] when the system refuses it. */
@Composable
fun rememberShareHandler(snackbarHostState: SnackbarHostState): (ShareSpec) -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    return remember(context, resources, snackbarHostState, scope) {
        { spec ->
            if (!context.shareImages(spec)) {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.core_ui_share_failed)) }
            }
        }
    }
}
