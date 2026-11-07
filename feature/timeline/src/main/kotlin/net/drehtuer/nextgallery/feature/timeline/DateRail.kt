package net.drehtuer.nextgallery.feature.timeline

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import net.drehtuer.nextgallery.core.common.ScrollbarMath
import net.drehtuer.nextgallery.core.data.timeline.DayRailDates
import net.drehtuer.nextgallery.core.data.timeline.TimelineIndex
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal const val DATE_RAIL_THUMB_TAG = "date-rail-thumb"
internal const val DATE_RAIL_CURRENT_TAG = "date-rail-current"
internal const val DATE_RAIL_ABOVE_TAG = "date-rail-above"
internal const val DATE_RAIL_BELOW_TAG = "date-rail-below"

/** English month and day names go with the English texts (D34), whatever the phone's language. */
@Composable
internal fun rememberDateFormatter(@StringRes pattern: Int): DateTimeFormatter {
    val text = stringResource(pattern)
    return remember(text) { DateTimeFormatter.ofPattern(text, Locale.ENGLISH) }
}

/** The row in the vertical middle of the grid: the last one that starts above the middle. */
internal fun middleRow(layoutInfo: LazyGridLayoutInfo): Int? {
    val middle = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return layoutInfo.visibleItemsInfo.lastOrNull { it.offset.y <= middle }?.index
}

/**
 * The timeline's fast scrollbar (D43): a track, a draggable thumb at the scroll position and, next to the thumb, the
 * day in the middle of the screen with its neighbours. It only recomposes when a date changes; the thumb moves in
 * the layout phase.
 */
@Composable
internal fun DateRail(state: LazyGridState, index: TimelineIndex, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    val thumbHeightPx = with(LocalDensity.current) { dimensions.scrollbarThumbHeight.roundToPx() }
    var trackHeightPx by remember { mutableIntStateOf(0) }
    var labelsHeightPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val currentIndex by rememberUpdatedState(index)
    val visibleCount by remember(state) { derivedStateOf { state.layoutInfo.visibleItemsInfo.size } }
    val fraction by remember(state) {
        derivedStateOf {
            dragFraction
                ?: ScrollbarMath.fractionForIndex(state.firstVisibleItemIndex, currentIndex.rowCount, visibleCount)
        }
    }
    val dates by remember(state) {
        derivedStateOf { middleRow(state.layoutInfo)?.let { currentIndex.dayRailAt(it) } }
    }

    // collectLatest drops positions the grid has not reached yet, so a long drag does not queue up jumps.
    LaunchedEffect(state) {
        snapshotFlow { dragFraction }.filterNotNull().collectLatest {
            state.scrollToItem(ScrollbarMath.indexForFraction(it, currentIndex.rowCount, visibleCount))
        }
    }

    Box(modifier.fillMaxSize().onSizeChanged { trackHeightPx = it.height }) {
        val travelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1)
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = (dimensions.scrollbarThumbWidth - dimensions.dateRailTrackWidth) / 2)
                .fillMaxHeight()
                .width(dimensions.dateRailTrackWidth)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        dates?.let { current ->
            RailLabels(
                dates = current,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = dimensions.dateRailInset)
                    .onSizeChanged { labelsHeightPx = it.height }
                    .offset {
                        val centred = (fraction * travelPx).roundToInt() + (thumbHeightPx - labelsHeightPx) / 2
                        IntOffset(0, centred.coerceIn(0, (trackHeightPx - labelsHeightPx).coerceAtLeast(0)))
                    },
            )
        }
        RailThumb(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, (fraction * travelPx).roundToInt()) },
            onDragStarted = { dragFraction = fraction },
            onDrag = { delta -> dragFraction = ((dragFraction ?: fraction) + delta / travelPx).coerceIn(0f, 1f) },
            onDragStopped = { dragFraction = null },
        )
    }
}

@Composable
private fun RailThumb(
    onDragStarted: () -> Unit,
    onDrag: (delta: Float) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = NextGalleryTheme.dimensions
    Box(
        modifier
            .size(dimensions.scrollbarTouchWidth, dimensions.scrollbarThumbHeight)
            .testTag(DATE_RAIL_THUMB_TAG)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState(onDrag),
                onDragStarted = { onDragStarted() },
                onDragStopped = { onDragStopped() },
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(dimensions.scrollbarThumbWidth)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
        )
    }
}

/** Plain text on backgrounds and no `Surface`, because a `Surface` would swallow taps on the photos below. */
@Composable
private fun RailLabels(dates: DayRailDates, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(dimensions.dateRailLabelPadding),
    ) {
        dates.above?.let { NeighbourLabel(it, Modifier.testTag(DATE_RAIL_ABOVE_TAG)) }
        Text(
            text = rememberDateFormatter(R.string.feature_timeline_rail_pattern).format(dates.current),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .testTag(DATE_RAIL_CURRENT_TAG)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                .padding(horizontal = dimensions.spacingSmall, vertical = dimensions.dateRailLabelPadding),
        )
        dates.below?.let { NeighbourLabel(it, Modifier.testTag(DATE_RAIL_BELOW_TAG)) }
    }
}

@Composable
private fun NeighbourLabel(date: LocalDate, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    val backing = MaterialTheme.colorScheme.surface.copy(alpha = dimensions.dateRailLabelOpacity)
    Text(
        text = rememberDateFormatter(R.string.feature_timeline_rail_pattern).format(date),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(backing, MaterialTheme.shapes.small)
            .padding(horizontal = dimensions.dateRailLabelPadding),
    )
}
