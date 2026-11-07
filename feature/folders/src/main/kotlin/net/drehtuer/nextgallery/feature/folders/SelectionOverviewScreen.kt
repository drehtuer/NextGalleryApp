package net.drehtuer.nextgallery.feature.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.data.selection.SelectionOverviewItem
import net.drehtuer.nextgallery.core.ui.component.BackButton
import net.drehtuer.nextgallery.core.ui.component.DeleteAction
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.component.ShareAction
import net.drehtuer.nextgallery.core.ui.component.UploadAction
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.thumbnail.LARGE_THUMBNAIL_PX
import net.drehtuer.nextgallery.core.ui.thumbnail.ThumbnailCell
import net.drehtuer.nextgallery.core.ui.thumbnail.thumbnailDescription
import net.drehtuer.nextgallery.core.ui.thumbnail.toThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionOverviewScreen(
    state: SelectionOverviewUiState,
    onEvent: (SelectionOverviewEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_folders_selection_overview)) },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    if (!state.items.isNullOrEmpty()) {
                        ShareAction(onClick = { onEvent(SelectionOverviewEvent.Share) })
                        UploadAction(state.uploadAvailability, onClick = { onEvent(SelectionOverviewEvent.Upload) })
                        DeleteAction(onClick = { onEvent(SelectionOverviewEvent.Delete) })
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.items == null -> LoadingIndicator()

                state.items.isEmpty() -> EmptyState(
                    title = stringResource(R.string.feature_folders_selection_empty),
                    detail = stringResource(R.string.feature_folders_selection_empty_detail),
                )

                else -> OverviewGrid(state.items, state.greyedOutForMissingLocation, onEvent)
            }
        }
    }
}

@Composable
private fun OverviewGrid(
    items: List<SelectionOverviewItem>,
    greyedOutForMissingLocation: Boolean,
    onEvent: (SelectionOverviewEvent) -> Unit,
) {
    val dimensions = NextGalleryTheme.dimensions
    LazyVerticalGrid(
        columns = GridCells.Fixed(dimensions.gridColumns),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(dimensions.gridGap),
        verticalArrangement = Arrangement.spacedBy(dimensions.gridGap),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { SelectionNote(items, greyedOutForMissingLocation) }
        items(items, key = { it.item.id.value }) { (item, greyedOut) ->
            ThumbnailCell(
                thumbnail = item.toThumbnail(),
                contentDescription = thumbnailDescription(item),
                sizePx = LARGE_THUMBNAIL_PX,
                selected = true,
                selecting = true,
                greyedOut = greyedOut,
                modifier = Modifier.clickable { onEvent(SelectionOverviewEvent.ItemClicked(item.id)) },
            )
        }
    }
}

@Composable
private fun SelectionNote(items: List<SelectionOverviewItem>, greyedOutForMissingLocation: Boolean) {
    val greyedOut = items.count { it.greyedOut }
    Text(
        text = if (greyedOut > 0) {
            pluralStringResource(
                if (greyedOutForMissingLocation) {
                    R.plurals.feature_folders_selection_note_greyed_map
                } else {
                    R.plurals.feature_folders_selection_note_greyed
                },
                items.size,
                items.size,
                greyedOut,
            )
        } else {
            pluralStringResource(R.plurals.feature_folders_selection_note, items.size, items.size)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(NextGalleryTheme.dimensions.spacingMedium),
    )
}
