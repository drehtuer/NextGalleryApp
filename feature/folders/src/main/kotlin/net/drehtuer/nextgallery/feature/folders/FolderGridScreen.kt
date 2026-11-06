package net.drehtuer.nextgallery.feature.folders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.ui.component.BackButton
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.ErrorState
import net.drehtuer.nextgallery.core.ui.component.FastScrollbar
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.component.MediaGrid
import net.drehtuer.nextgallery.core.ui.component.SelectionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderGridScreen(
    state: FolderGridUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (FolderGridEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selecting = state.selectedIds.isNotEmpty()
    BackHandler(enabled = selecting) { onEvent(FolderGridEvent.ClearSelection) }
    Scaffold(
        modifier = modifier,
        topBar = {
            if (selecting) {
                SelectionTopBar(
                    selectionCount = state.selectedIds.size,
                    onClear = { onEvent(FolderGridEvent.ClearSelection) },
                    actions = { SelectAllButton(onClick = { onEvent(FolderGridEvent.SelectAll) }) },
                )
            } else {
                TopAppBar(title = { Text(state.title) }, navigationIcon = { BackButton(onClick = onBack) })
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
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
            )
            FastScrollbar(gridState, items.itemCount, Modifier.align(Alignment.CenterEnd))
        }

        refresh is LoadState.Error -> ErrorState(stringResource(R.string.feature_folders_load_failed), items::retry)

        refresh is LoadState.Loading -> LoadingIndicator()

        else -> EmptyState(stringResource(R.string.feature_folders_folder_empty))
    }
}

@Composable
private fun SelectAllButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.feature_folders_ic_select_all),
            contentDescription = stringResource(R.string.feature_folders_select_all),
        )
    }
}
