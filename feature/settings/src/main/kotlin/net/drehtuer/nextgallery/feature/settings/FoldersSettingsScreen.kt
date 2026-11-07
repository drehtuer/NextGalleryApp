package net.drehtuer.nextgallery.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import net.drehtuer.nextgallery.core.model.AddedFolder
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.VirtualFolderSummary
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

/** [onAddFolder] opens the system folder picker, whose result comes back as [FoldersSettingsEvent.FolderPicked]. */
@Composable
fun FoldersSettingsScreen(
    state: FoldersSettingsUiState,
    onEvent: (FoldersSettingsEvent) -> Unit,
    onAddFolder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editor = (state as? FoldersSettingsUiState.Content)?.editor
    if (state is FoldersSettingsUiState.Content && editor != null) {
        VirtualFolderEditorContent(editor, state.folders, onEvent, modifier)
    } else {
        SettingsScaffold(stringResource(R.string.feature_settings_folders), onBack, modifier) { contentModifier ->
            when (state) {
                FoldersSettingsUiState.Loading -> LoadingIndicator(contentModifier)
                is FoldersSettingsUiState.Content -> FoldersList(state, onEvent, onAddFolder, contentModifier)
            }
        }
    }
}

@Composable
private fun FoldersList(
    state: FoldersSettingsUiState.Content,
    onEvent: (FoldersSettingsEvent) -> Unit,
    onAddFolder: () -> Unit,
    modifier: Modifier,
) {
    val dimensions = NextGalleryTheme.dimensions
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        item { Text(stringResource(R.string.feature_settings_folders_intro)) }
        if (state.addedFolders.isEmpty()) {
            item { SettingsNote(stringResource(R.string.feature_settings_folders_none)) }
        }
        items(state.addedFolders, key = { "${it.volumeName}:${it.relativePath}" }) { folder ->
            AddedFolderRow(folder, onRemove = { onEvent(FoldersSettingsEvent.RemoveFolder(folder)) })
        }
        item {
            Button(onClick = onAddFolder) { Text(stringResource(R.string.feature_settings_folder_add)) }
        }
        if (state.addFailed) {
            item {
                Text(
                    stringResource(R.string.feature_settings_folder_add_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item { SettingsNote(stringResource(R.string.feature_settings_folders_note)) }
        item { SectionTitle(stringResource(R.string.feature_settings_virtual_folders)) }
        item { SettingsNote(stringResource(R.string.feature_settings_virtual_folders_intro)) }
        items(state.virtualFolders, key = { it.folder.id }) { summary ->
            VirtualFolderRow(summary, onEvent)
        }
        item {
            OutlinedButton(
                onClick = { onEvent(FoldersSettingsEvent.CreateVirtualFolder) },
                enabled = state.folders.isNotEmpty(),
            ) { Text(stringResource(R.string.feature_settings_virtual_folder_create)) }
        }
        if (state.folders.isEmpty()) {
            item { SettingsNote(stringResource(R.string.feature_settings_virtual_folders_needs_folder)) }
        }
    }
}

@Composable
private fun AddedFolderRow(folder: AddedFolder, onRemove: () -> Unit) {
    val removeDescription = stringResource(R.string.feature_settings_folder_remove_description, folder.name)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (folder.isOnPrimaryVolume) {
                    folder.relativePath
                } else {
                    stringResource(R.string.feature_settings_folder_on_volume, folder.volumeName, folder.relativePath)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
        }
        TextButton(
            onClick = onRemove,
            modifier = Modifier.semantics { contentDescription = removeDescription },
        ) { Text(stringResource(R.string.feature_settings_folder_remove)) }
    }
}

@Composable
private fun VirtualFolderRow(summary: VirtualFolderSummary, onEvent: (FoldersSettingsEvent) -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    val folder = summary.folder
    val deleteDescription = stringResource(R.string.feature_settings_virtual_folder_delete_description, folder.name)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .heightIn(min = dimensions.minTouchTarget)
                .clickable { onEvent(FoldersSettingsEvent.EditVirtualFolder(folder)) },
        ) {
            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = pluralStringResource(
                    R.plurals.feature_settings_virtual_folder_count,
                    summary.itemCount,
                    summary.itemCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = { onEvent(FoldersSettingsEvent.DeleteVirtualFolder(folder.id)) },
            modifier = Modifier.semantics { contentDescription = deleteDescription },
        ) { Text(stringResource(R.string.feature_settings_virtual_folder_delete)) }
    }
}

@Composable
private fun VirtualFolderEditorContent(
    editor: VirtualFolderEditor,
    folders: List<Folder>,
    onEvent: (FoldersSettingsEvent) -> Unit,
    modifier: Modifier,
) {
    val dimensions = NextGalleryTheme.dimensions
    val close = { onEvent(FoldersSettingsEvent.EditorClosed) }
    BackHandler(onBack = close)
    val title = if (editor.id == null) {
        R.string.feature_settings_virtual_folder_new
    } else {
        R.string.feature_settings_virtual_folder_edit
    }
    SettingsScaffold(stringResource(title), close, modifier) { contentModifier ->
        LazyColumn(
            modifier = contentModifier.imePadding(),
            contentPadding = PaddingValues(dimensions.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        ) {
            item {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { onEvent(FoldersSettingsEvent.EditorNameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.feature_settings_virtual_folder_name)) },
                    singleLine = true,
                )
            }
            item { SectionTitle(stringResource(R.string.feature_settings_virtual_folder_members)) }
            items(folders, key = { it.bucketId }) { folder ->
                MemberRow(
                    folder = folder,
                    checked = folder.bucketId in editor.bucketIds,
                    onToggle = { onEvent(FoldersSettingsEvent.EditorMemberToggled(folder.bucketId)) },
                )
            }
            item {
                Button(onClick = { onEvent(FoldersSettingsEvent.EditorSaved) }, enabled = editor.canSave) {
                    Text(stringResource(R.string.feature_settings_virtual_folder_save))
                }
            }
        }
    }
}

@Composable
private fun MemberRow(folder: Folder, checked: Boolean, onToggle: () -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensions.minTouchTarget)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = folder.relativePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
        }
    }
}
