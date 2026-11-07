package net.drehtuer.nextgallery.feature.folders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.ui.component.BackButton
import net.drehtuer.nextgallery.core.ui.component.DeleteAction
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.ErrorState
import net.drehtuer.nextgallery.core.ui.component.FastScrollbar
import net.drehtuer.nextgallery.core.ui.component.MediaGrid
import net.drehtuer.nextgallery.core.ui.component.MediaGridPlaceholder
import net.drehtuer.nextgallery.core.ui.component.ScreenBanner
import net.drehtuer.nextgallery.core.ui.component.SelectAction
import net.drehtuer.nextgallery.core.ui.component.SelectAllAction
import net.drehtuer.nextgallery.core.ui.component.SelectionOverviewAction
import net.drehtuer.nextgallery.core.ui.component.SelectionTopBar
import net.drehtuer.nextgallery.core.ui.component.ShareAction
import net.drehtuer.nextgallery.core.ui.component.UploadAction
import net.drehtuer.nextgallery.core.ui.component.VisibleMediaIdsEffect
import net.drehtuer.nextgallery.core.ui.thumbnail.UploadBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderGridScreen(
    state: FolderGridUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (FolderGridEvent) -> Unit,
    onBack: () -> Unit,
    onOpenSelectionOverview: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    BackHandler(enabled = state.selecting) { onEvent(FolderGridEvent.ClearSelection) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.selecting) {
                SelectionTopBar(
                    selectionCount = state.selectedIds.size,
                    onClear = { onEvent(FolderGridEvent.ClearSelection) },
                    actions = {
                        SelectAllAction(onClick = { onEvent(FolderGridEvent.SelectAll) })
                        SelectionOverviewAction(onClick = onOpenSelectionOverview)
                        ShareAction(onClick = { onEvent(FolderGridEvent.ShareSelected) })
                        UploadAction(state.uploadAvailability, onClick = { onEvent(FolderGridEvent.UploadSelected) })
                        DeleteAction(onClick = { onEvent(FolderGridEvent.DeleteSelected) })
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(state.title) },
                    navigationIcon = { BackButton(onClick = onBack) },
                    actions = { SelectAction(onClick = { onEvent(FolderGridEvent.StartSelecting) }) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScreenBanner()
            FolderGridContent(state, items, onEvent)
        }
    }
}

@Composable
private fun FolderGridContent(
    state: FolderGridUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (FolderGridEvent) -> Unit,
) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount > 0 -> Box(Modifier.fillMaxSize()) {
            val gridState = rememberLazyGridState()
            MediaGrid(
                items = items,
                selectedIds = state.selectedIds,
                onItemClick = { onEvent(FolderGridEvent.ItemClicked(it.id)) },
                onItemLongClick = { onEvent(FolderGridEvent.ItemLongPressed(it.id)) },
                state = gridState,
                selecting = state.selecting,
                cellBadge = { item -> state.uploadBadges[item.id]?.let { UploadBadge(it) } },
            )
            VisibleMediaIdsEffect(gridState) { onEvent(FolderGridEvent.VisibleItemsChanged(it)) }
            FastScrollbar(gridState, items.itemCount, Modifier.align(Alignment.CenterEnd))
        }

        refresh is LoadState.Error -> ErrorState(
            title = stringResource(R.string.feature_folders_load_failed),
            message = stringResource(R.string.feature_folders_load_failed_detail),
            onRetry = items::retry,
        )

        refresh is LoadState.Loading -> MediaGridPlaceholder()

        else -> EmptyState(
            title = stringResource(R.string.feature_folders_folder_empty),
            detail = stringResource(R.string.feature_folders_folder_empty_detail),
        )
    }
}
