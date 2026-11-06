package net.drehtuer.nextgallery.core.ui.platform

import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.drehtuer.nextgallery.core.model.PlatformConfirmation

/** Shows a system confirmation dialog; [onResult] tells whether the user agreed. */
@Composable
fun rememberConfirmationLauncher(onResult: (confirmed: Boolean) -> Unit): (PlatformConfirmation) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        onResult(it.resultCode == Activity.RESULT_OK)
    }
    return remember(launcher) {
        { confirmation -> launcher.launch(IntentSenderRequest.Builder(confirmation.handle as IntentSender).build()) }
    }
}
