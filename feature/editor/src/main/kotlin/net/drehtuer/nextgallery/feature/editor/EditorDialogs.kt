package net.drehtuer.nextgallery.feature.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
internal fun EditorDialogs(state: EditorUiState, onEvent: (EditorEvent) -> Unit) {
    val size = state.imageSize
    if (state.isScaleDialogShown && size != null) {
        ScaleDialog(
            original = size,
            onApply = { onEvent(EditorEvent.ApplyScale(it)) },
            onDismiss = { onEvent(EditorEvent.HideScaleDialog) },
        )
    }
    if (state.isDiscardDialogShown) DiscardDialog(state.displayName, onEvent)
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = { onEvent(EditorEvent.DismissMessage) },
            confirmButton = {
                TextButton(onClick = { onEvent(EditorEvent.DismissMessage) }) {
                    Text(stringResource(R.string.feature_editor_ok))
                }
            },
            text = { Text(messageText(message)) },
        )
    }
}

@Composable
private fun DiscardDialog(displayName: String, onEvent: (EditorEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(EditorEvent.KeepEditing) },
        title = { Text(stringResource(R.string.feature_editor_discard_title)) },
        text = { Text(stringResource(R.string.feature_editor_discard_message, displayName)) },
        confirmButton = {
            TextButton(onClick = { onEvent(EditorEvent.Discard) }) {
                Text(stringResource(R.string.feature_editor_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(EditorEvent.KeepEditing) }) {
                Text(stringResource(R.string.feature_editor_keep_editing))
            }
        },
    )
}

@Composable
private fun messageText(message: EditorMessage): String = when (message) {
    is EditorMessage.NotEnoughMemory -> stringResource(
        R.string.feature_editor_not_enough_memory,
        message.requiredMegabytes,
        message.availableMegabytes,
    )

    EditorMessage.SaveFailed -> stringResource(R.string.feature_editor_save_failed)

    EditorMessage.PreviewFailed -> stringResource(R.string.feature_editor_preview_failed)
}
