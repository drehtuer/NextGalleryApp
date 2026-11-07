package net.drehtuer.nextgallery.feature.timeline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import net.drehtuer.nextgallery.core.data.timeline.TimelineIndex
import net.drehtuer.nextgallery.core.data.timeline.TimelineRow
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.ui.component.DeleteAction
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.ErrorState
import net.drehtuer.nextgallery.core.ui.component.MediaGridPlaceholder
import net.drehtuer.nextgallery.core.ui.component.SelectAction
import net.drehtuer.nextgallery.core.ui.component.SelectAllAction
import net.drehtuer.nextgallery.core.ui.component.SelectionOverviewAction
import net.drehtuer.nextgallery.core.ui.component.SelectionTopBar
import net.drehtuer.nextgallery.core.ui.component.ShareAction
import net.drehtuer.nextgallery.core.ui.component.UploadAction
import net.drehtuer.nextgallery.core.ui.component.VisibleMediaIdsEffect
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.thumbnail.ThumbnailCell
import net.drehtuer.nextgallery.core.ui.thumbnail.UploadBadge
import net.drehtuer.nextgallery.core.ui.thumbnail.thumbnailDescription
import net.drehtuer.nextgallery.core.ui.thumbnail.thumbnailSizePx
import net.drehtuer.nextgallery.core.ui.thumbnail.toThumbnail
import java.time.LocalDate
import java.time.YearMonth
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

internal const val TIMELINE_GRID_TAG = "timeline-grid"
internal const val STICKY_MONTH_TAG = "timeline-sticky-month"

private const val MONTH_CONTENT_TYPE = "month"
private const val DAY_CONTENT_TYPE = "day"
private const val PHOTO_CONTENT_TYPE = "photo"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    state: TimelineUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (TimelineEvent) -> Unit,
    onOpenSelectionOverview: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    BackHandler(enabled = state.selecting) { onEvent(TimelineEvent.ClearSelection) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.selecting) {
                SelectionTopBar(
                    selectionCount = state.selectedIds.size,
                    onClear = { onEvent(TimelineEvent.ClearSelection) },
                    actions = {
                        SelectAllAction(onClick = { onEvent(TimelineEvent.SelectAll) })
                        SelectionOverviewAction(onClick = onOpenSelectionOverview)
                        ShareAction(onClick = { onEvent(TimelineEvent.ShareSelected) })
                        UploadAction(state.uploadAvailability, onClick = { onEvent(TimelineEvent.UploadSelected) })
                        DeleteAction(onClick = { onEvent(TimelineEvent.DeleteSelected) })
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.feature_timeline_title)) },
                    actions = { SelectAction(onClick = { onEvent(TimelineEvent.StartSelecting) }) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            TimelineContent(state, items, onEvent)
        }
    }
}

@Composable
private fun TimelineContent(
    state: TimelineUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (TimelineEvent) -> Unit,
) {
    val index = state.index
    when {
        items.itemCount == 0 && items.loadState.refresh is LoadState.Error -> ErrorState(
            title = stringResource(R.string.feature_timeline_load_failed),
            message = stringResource(R.string.feature_timeline_load_failed_detail),
            onRetry = items::retry,
        )

        index == null -> MediaGridPlaceholder()

        index.isEmpty -> EmptyState(
            title = stringResource(R.string.feature_timeline_empty),
            detail = stringResource(R.string.feature_timeline_empty_detail),
        )

        else -> Box(Modifier.fillMaxSize()) {
            val gridState = rememberLazyGridState()
            TimelineGrid(index, state, items, gridState, onEvent)
            VisibleMediaIdsEffect(gridState) { onEvent(TimelineEvent.VisibleItemsChanged(it)) }
            StickyMonth(index, gridState)
            // A sibling after the list and the sticky header, so nothing covers it (D43).
            DateRail(gridState, index)
        }
    }
}

/** Headers come from [index] and images from [items]; the two lists can differ for a moment after a change. */
@Composable
private fun TimelineGrid(
    index: TimelineIndex,
    state: TimelineUiState,
    items: LazyPagingItems<MediaItem>,
    gridState: LazyGridState,
    onEvent: (TimelineEvent) -> Unit,
) {
    val dimensions = NextGalleryTheme.dimensions
    val columns = dimensions.gridColumns
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sizePx = with(LocalDensity.current) { thumbnailSizePx(maxWidth.roundToPx(), columns) }
        val photoKey = items.itemKey { it.id.value }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize().testTag(TIMELINE_GRID_TAG),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(dimensions.gridGap),
            verticalArrangement = Arrangement.spacedBy(dimensions.gridGap),
        ) {
            items(
                count = index.rowCount,
                key = { position -> rowKey(index.rowAt(position), items.itemCount, photoKey) },
                span = { position -> GridItemSpan(if (index.isHeader(position)) maxLineSpan else 1) },
                contentType = { position -> rowContentType(index.rowAt(position)) },
            ) { position ->
                when (val row = index.rowAt(position)) {
                    is TimelineRow.MonthHeader -> MonthHeader(row.month)
                    is TimelineRow.DayHeader -> DayHeader(row.date, state.today)
                    is TimelineRow.Photo -> PhotoCell(row, items, state, sizePx, onEvent)
                }
            }
        }
    }
}

private fun rowKey(row: TimelineRow, photoCount: Int, photoKey: (Int) -> Any): Any = when (row) {
    is TimelineRow.MonthHeader -> "month-${row.month}"
    is TimelineRow.DayHeader -> "day-${row.date}"
    is TimelineRow.Photo -> if (row.photoIndex < photoCount) photoKey(row.photoIndex) else "photo-${row.photoIndex}"
}

private fun rowContentType(row: TimelineRow): String = when (row) {
    is TimelineRow.MonthHeader -> MONTH_CONTENT_TYPE
    is TimelineRow.DayHeader -> DAY_CONTENT_TYPE
    is TimelineRow.Photo -> PHOTO_CONTENT_TYPE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCell(
    row: TimelineRow.Photo,
    items: LazyPagingItems<MediaItem>,
    state: TimelineUiState,
    sizePx: Int,
    onEvent: (TimelineEvent) -> Unit,
) {
    val item = if (row.photoIndex < items.itemCount) items[row.photoIndex] else null
    ThumbnailCell(
        thumbnail = item?.toThumbnail(),
        contentDescription = item?.let { thumbnailDescription(it) },
        sizePx = sizePx,
        selected = item?.id in state.selectedIds,
        selecting = state.selecting,
        modifier = Modifier.combinedClickable(
            enabled = item != null,
            onLongClickLabel = stringResource(CoreUiR.string.core_ui_select),
            onClick = { item?.let { onEvent(TimelineEvent.ItemClicked(it.id)) } },
            onLongClick = { item?.let { onEvent(TimelineEvent.ItemLongPressed(it.id)) } },
        ),
        badge = { item?.let { state.uploadBadges[it.id] }?.let { UploadBadge(it) } },
    )
}

/** The month of the first visible row, over the grid. The grid's own header of that month scrolls away under it. */
@Composable
private fun StickyMonth(index: TimelineIndex, gridState: LazyGridState) {
    val currentIndex by rememberUpdatedState(index)
    val month by remember(gridState) { derivedStateOf { currentIndex.monthAt(gridState.firstVisibleItemIndex) } }
    MonthHeader(month, Modifier.testTag(STICKY_MONTH_TAG))
}

@Composable
private fun MonthHeader(month: YearMonth, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    Text(
        text = rememberDateFormatter(R.string.feature_timeline_month_pattern).format(month),
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall),
    )
}

@Composable
private fun DayHeader(date: LocalDate, today: LocalDate?) {
    val dimensions = NextGalleryTheme.dimensions
    val day = rememberDateFormatter(R.string.feature_timeline_day_pattern).format(date)
    Text(
        text = if (date == today) stringResource(R.string.feature_timeline_today, day) else day,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall),
    )
}
