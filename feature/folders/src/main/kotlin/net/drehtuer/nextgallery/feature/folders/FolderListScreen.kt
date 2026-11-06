package net.drehtuer.nextgallery.feature.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.ScreenBanner
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
fun FolderListScreen(state: FolderListUiState, onFolderClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_folders_title)) }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScreenBanner()
            when (state) {
                FolderListUiState.Loading -> FolderListPlaceholder()

                FolderListUiState.Empty -> EmptyState(
                    title = stringResource(R.string.feature_folders_empty),
                    detail = stringResource(R.string.feature_folders_empty_detail),
                    icon = CoreUiR.drawable.core_ui_ic_photo,
                )

                is FolderListUiState.Content -> FolderGrid(state.folders, onFolderClick)
            }
        }
    }
}

@Composable
private fun FolderGrid(folders: List<Folder>, onFolderClick: (Long) -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    LazyVerticalGrid(
        columns = GridCells.Adaptive(dimensions.folderCellMinSize),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimensions.spacingSmall),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
    ) {
        items(folders, key = { it.bucketId }) { folder ->
            FolderCell(folder, onClick = { onFolderClick(folder.bucketId) })
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
private fun FolderCell(folder: Folder, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        ThumbnailCell(
            thumbnail = folder.coverItem.toThumbnail(),
            contentDescription = null,
            sizePx = LARGE_THUMBNAIL_PX,
            modifier = Modifier.clip(MaterialTheme.shapes.large),
        )
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(R.plurals.feature_folders_item_count, folder.itemCount, folder.itemCount),
            style = MaterialTheme.typography.bodySmall.tabularFigures,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
