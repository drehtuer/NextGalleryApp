package net.drehtuer.nextgallery.feature.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.ScreenBanner
import net.drehtuer.nextgallery.core.ui.component.SettingsAction
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.theme.tabularFigures
import net.drehtuer.nextgallery.core.ui.thumbnail.LARGE_THUMBNAIL_PX
import net.drehtuer.nextgallery.core.ui.thumbnail.ThumbnailCell
import net.drehtuer.nextgallery.core.ui.thumbnail.toThumbnail
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

internal const val FOLDER_LIST_PLACEHOLDER_TAG = "folder-list-placeholder"
private const val PLACEHOLDER_FOLDERS = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    state: FolderListUiState,
    onFolderClick: (Long) -> Unit,
    onVirtualFolderClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolderSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_folders_title)) },
                actions = { SettingsAction(onClick = onOpenSettings) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScreenBanner()
            when (state) {
                FolderListUiState.Loading -> FolderListPlaceholder()

                FolderListUiState.NoFolders -> EmptyFolderList(
                    title = stringResource(R.string.feature_folders_no_folders),
                    detail = stringResource(R.string.feature_folders_no_folders_detail),
                    actionLabel = stringResource(R.string.feature_folders_add_folder),
                    onAction = onOpenFolderSettings,
                )

                FolderListUiState.NoImages -> EmptyFolderList(
                    title = stringResource(R.string.feature_folders_no_images),
                    detail = stringResource(R.string.feature_folders_no_images_detail),
                    actionLabel = stringResource(R.string.feature_folders_manage_folders),
                    onAction = onOpenFolderSettings,
                )

                is FolderListUiState.Content -> FolderGrid(state, onFolderClick, onVirtualFolderClick)
            }
        }
    }
}

@Composable
private fun EmptyFolderList(title: String, detail: String, actionLabel: String, onAction: () -> Unit) {
    EmptyState(
        title = title,
        detail = detail,
        icon = CoreUiR.drawable.core_ui_ic_photo,
        action = { Button(onClick = onAction) { Text(actionLabel) } },
    )
}

@Composable
private fun FolderGrid(
    state: FolderListUiState.Content,
    onFolderClick: (Long) -> Unit,
    onVirtualFolderClick: (String) -> Unit,
) {
    val dimensions = NextGalleryTheme.dimensions
    LazyVerticalGrid(
        columns = GridCells.Adaptive(dimensions.folderCellMinSize),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimensions.spacingSmall),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
    ) {
        items(state.virtualFolders, key = { it.folder.id }) { summary ->
            FolderCell(
                name = summary.folder.name,
                itemCount = summary.itemCount,
                cover = summary.coverItem,
                virtual = true,
                onClick = { onVirtualFolderClick(summary.folder.id) },
            )
        }
        items(state.folders, key = { it.bucketId }) { folder ->
            FolderCell(
                name = folder.name,
                itemCount = folder.itemCount,
                cover = folder.coverItem,
                virtual = false,
                onClick = { onFolderClick(folder.bucketId) },
            )
        }
    }
}

/** Flat grey covers in the list's layout while the folders load. */
@Composable
private fun FolderListPlaceholder() {
    val dimensions = NextGalleryTheme.dimensions
    LazyVerticalGrid(
        columns = GridCells.Adaptive(dimensions.folderCellMinSize),
        modifier = Modifier.fillMaxSize().testTag(FOLDER_LIST_PLACEHOLDER_TAG),
        contentPadding = PaddingValues(dimensions.spacingSmall),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
        userScrollEnabled = false,
    ) {
        items(PLACEHOLDER_FOLDERS) {
            ThumbnailCell(
                thumbnail = null,
                contentDescription = null,
                sizePx = LARGE_THUMBNAIL_PX,
                modifier = Modifier.clip(MaterialTheme.shapes.large),
            )
        }
    }
}

@Composable
private fun FolderCell(name: String, itemCount: Int, cover: MediaItem?, virtual: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        ThumbnailCell(
            thumbnail = cover?.toThumbnail(),
            contentDescription = null,
            sizePx = LARGE_THUMBNAIL_PX,
            modifier = Modifier.clip(MaterialTheme.shapes.large),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NextGalleryTheme.dimensions.spacingSmall),
        ) {
            if (virtual) {
                Icon(
                    painterResource(R.drawable.feature_folders_ic_virtual_folder),
                    contentDescription = stringResource(R.string.feature_folders_virtual_folder),
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = pluralStringResource(R.plurals.feature_folders_item_count, itemCount, itemCount),
            style = MaterialTheme.typography.bodySmall.tabularFigures,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
