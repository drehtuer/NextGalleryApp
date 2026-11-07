package net.drehtuer.nextgallery.feature.remote

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

/**
 * The server has no trash bin, or it could not be checked, so the images would be gone for good (D56). As in the
 * certificate dialog, the irreversible choice is the less prominent button, in the error colour; closing the dialog in
 * another way cancels.
 */
@Composable
internal fun DeleteConfirmationDialog(state: DeleteConfirmationState, onEvent: (RemoteBrowserEvent) -> Unit) {
    val count = state.items.size
    AlertDialog(
        onDismissRequest = { onEvent(ActionEvent.CancelDelete) },
        title = { Text(stringResource(R.string.feature_remote_delete_permanently_title)) },
        text = { Text(pluralStringResource(R.plurals.feature_remote_delete_permanently_message, count, count)) },
        confirmButton = {
            Button(onClick = { onEvent(ActionEvent.CancelDelete) }) {
                Text(stringResource(CoreUiR.string.core_ui_cancel))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(ActionEvent.ConfirmDelete) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(CoreUiR.string.core_ui_delete))
            }
        },
    )
}
