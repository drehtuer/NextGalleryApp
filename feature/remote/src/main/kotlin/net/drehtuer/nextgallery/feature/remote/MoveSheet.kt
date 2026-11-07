package net.drehtuer.nextgallery.feature.remote

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.data.nextcloud.ConflictPolicy
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.ui.remote.Breadcrumbs
import net.drehtuer.nextgallery.core.ui.remote.RemoteFolderRow
import net.drehtuer.nextgallery.core.ui.remote.remoteMessage
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoveSheet(state: MoveSheetState, onEvent: (RemoteBrowserEvent) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onEvent(MoveEvent.SheetDismissed) }) { MoveSheetContent(state, onEvent) }
    if (state.showNewFolder) NewFolderDialog(onEvent)
}

@Composable
internal fun MoveSheetContent(state: MoveSheetState, onEvent: (RemoteBrowserEvent) -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        Column(Modifier.padding(horizontal = dimensions.spacingMedium)) {
            Text(
                pluralStringResource(R.plurals.feature_remote_move_title, state.items.size, state.items.size),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.feature_remote_move_intro), style = MaterialTheme.typography.bodyMedium)
        }
        Breadcrumbs(state.path, onOpen = { onEvent(MoveEvent.SheetFolderOpened(it)) })
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { MoveNote(stringResource(it.remoteMessage())) }
        state.folders.forEach { folder ->
            RemoteFolderRow(
                name = folder.item.name,
                detail = stringResource(R.string.feature_remote_move_read_only).takeUnless { folder.canWrite },
                enabled = folder.canWrite,
                onClick = { onEvent(MoveEvent.SheetFolderOpened(folder.item.path)) },
            )
        }
        TextButton(
            onClick = { onEvent(MoveEvent.NewFolderRequested) },
            enabled = state.canWriteHere && !state.isLoading,
            modifier = Modifier.padding(horizontal = dimensions.spacingSmall),
        ) {
            Text(stringResource(R.string.feature_remote_move_new_folder))
        }
        MoveSheetFooter(state, onEvent)
    }
}

@Composable
private fun MoveSheetFooter(state: MoveSheetState, onEvent: (RemoteBrowserEvent) -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    val root = stringResource(CoreUiR.string.core_ui_remote_root)
    val target = RemotePaths.segments(state.path).joinToString("/").ifEmpty { root }
    if (state.isSourceFolder) MoveNote(stringResource(R.string.feature_remote_move_already_here))
    if (!state.canWriteHere) MoveNote(stringResource(R.string.feature_remote_move_cannot_write))
    MoveNote(stringResource(R.string.feature_remote_move_to, target))
    Row(
        Modifier.fillMaxWidth().padding(dimensions.spacingMedium),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall, Alignment.End),
    ) {
        TextButton(onClick = { onEvent(MoveEvent.SheetDismissed) }) {
            Text(stringResource(CoreUiR.string.core_ui_cancel))
        }
        Button(onClick = { onEvent(MoveEvent.MoveHere) }, enabled = state.canMoveHere) {
            Text(stringResource(R.string.feature_remote_move_here))
        }
    }
}

@Composable
private fun MoveNote(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = NextGalleryTheme.dimensions.spacingMedium),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun NewFolderDialog(onEvent: (RemoteBrowserEvent) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onEvent(MoveEvent.NewFolderDismissed) },
        title = { Text(stringResource(R.string.feature_remote_move_new_folder)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.feature_remote_move_folder_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = {
                onEvent(MoveEvent.NewFolderConfirmed(name))
            }, enabled = RemotePaths.isValidName(name.trim())) {
                Text(stringResource(R.string.feature_remote_move_create))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(MoveEvent.NewFolderDismissed) }) {
                Text(stringResource(CoreUiR.string.core_ui_cancel))
            }
        },
    )
}

/** Asks per file (D46). Closing the dialog in another way skips the image: nothing on the server is touched. */
@Composable
internal fun MoveConflictDialog(state: MoveConflictState, onEvent: (RemoteBrowserEvent) -> Unit) {
    val root = stringResource(CoreUiR.string.core_ui_remote_root)
    val folder = RemotePaths.name(state.targetFolder).ifEmpty { root }
    var applyToAll by remember(state.item.path) { mutableStateOf(false) }
    val resolve = { policy: ConflictPolicy -> onEvent(MoveEvent.ConflictResolved(policy, applyToAll)) }
    AlertDialog(
        onDismissRequest = { resolve(ConflictPolicy.SKIP) },
        title = { Text(stringResource(R.string.feature_remote_conflict_title, state.item.name, folder)) },
        text = { ConflictDetails(state, folder, applyToAll, onApplyToAllChanged = { applyToAll = it }) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = { resolve(ConflictPolicy.KEEP_BOTH) }) {
                    Text(stringResource(R.string.feature_remote_conflict_keep_both))
                }
                TextButton(onClick = { resolve(ConflictPolicy.REPLACE) }) {
                    Text(stringResource(R.string.feature_remote_conflict_replace, folder))
                }
                TextButton(onClick = { resolve(ConflictPolicy.SKIP) }) {
                    Text(stringResource(R.string.feature_remote_conflict_skip))
                }
            }
        },
    )
}

@Composable
private fun ConflictDetails(
    state: MoveConflictState,
    folder: String,
    applyToAll: Boolean,
    onApplyToAllChanged: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NextGalleryTheme.dimensions.spacingSmall)) {
        val moved = state.item
        Text(stringResource(R.string.feature_remote_conflict_moved_file, formatDate(moved), formatSize(moved)))
        state.existing?.let { existing ->
            Text(
                stringResource(
                    R.string.feature_remote_conflict_existing_file,
                    folder,
                    formatDate(existing),
                    formatSize(existing),
                ),
            )
        }
        if (state.otherImages > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = applyToAll, onCheckedChange = onApplyToAllChanged)
                Text(
                    pluralStringResource(
                        R.plurals.feature_remote_conflict_apply_to_all,
                        state.otherImages,
                        state.otherImages,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun formatSize(item: RemoteItem): String = Formatter.formatFileSize(LocalContext.current, item.size)

internal fun formatDate(item: RemoteItem): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(item.lastModifiedMillis))

@Composable
internal fun ShareProgressDialog(state: RemoteShareUiState, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NextGalleryTheme.dimensions.spacingSmall)) {
                LinearProgressIndicator(
                    progress = { state.current.toFloat() / state.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.feature_remote_share_progress, state.current, state.total))
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text(stringResource(CoreUiR.string.core_ui_cancel)) }
        },
    )
}
