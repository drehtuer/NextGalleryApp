package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.ScrollbarMath
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import kotlin.math.roundToInt

internal const val FAST_SCROLLBAR_THUMB_TAG = "fast-scrollbar-thumb"

/**
 * Draggable scrollbar for a lazy grid, placed over the grid's end edge. It jumps directly to the target
 * position and only appears when the items do not fit on the screen. While dragging, [label] can show a
 * text for the first visible item (e.g. the month in the timeline).
 */
@Composable
fun FastScrollbar(
    state: LazyGridState,
    itemCount: Int,
    modifier: Modifier = Modifier,
    label: (index: Int) -> String? = { null },
) {
    val visibleCount by remember(state) { derivedStateOf { state.layoutInfo.visibleItemsInfo.size } }
    if (visibleCount == 0 || itemCount <= visibleCount) return

    val dimensions = NextGalleryTheme.dimensions
    val thumbHeightPx = with(LocalDensity.current) { dimensions.scrollbarThumbHeight.roundToPx() }
    var trackHeightPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val currentItemCount by rememberUpdatedState(itemCount)
    val currentVisibleCount by rememberUpdatedState(visibleCount)
    val currentLabel by rememberUpdatedState(label)
    val scope = rememberCoroutineScope()
    // Derived, so that scrolling only recomposes this when the thumb or the label really change.
    val fraction by remember(state) {
        derivedStateOf {
            dragFraction
                ?: ScrollbarMath.fractionForIndex(state.firstVisibleItemIndex, currentItemCount, currentVisibleCount)
        }
    }
    val labelText by remember(state) {
        derivedStateOf { if (dragFraction == null) null else currentLabel(state.firstVisibleItemIndex) }
    }

    // collectLatest drops positions the grid has not reached yet, so a long drag does not queue up jumps.
    LaunchedEffect(state) {
        snapshotFlow { dragFraction }.filterNotNull().collectLatest {
            state.scrollToItem(ScrollbarMath.indexForFraction(it, currentItemCount, currentVisibleCount))
        }
    }

    Box(modifier.fillMaxHeight().onSizeChanged { trackHeightPx = it.height }) {
        val travelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1)
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, (fraction * travelPx).roundToInt()) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labelText?.let { ScrollbarLabel(it) }
            ScrollbarThumb(
                modifier = Modifier.scrollbarSemantics(
                    description = stringResource(R.string.core_ui_scroll_position),
                    fraction = { fraction },
                    positionText = { currentLabel(state.firstVisibleItemIndex) },
                    onScrollTo = { target ->
                        val index = ScrollbarMath.indexForFraction(target, currentItemCount, currentVisibleCount)
                        scope.launch { state.scrollToItem(index) }
                    },
                ),
                onDragStarted = { dragFraction = fraction },
                onDrag = { delta -> dragFraction = ((dragFraction ?: fraction) + delta / travelPx).coerceIn(0f, 1f) },
                onDragStopped = { dragFraction = null },
            )
        }
    }
}

@Composable
private fun ScrollbarThumb(
    onDragStarted: () -> Unit,
    onDrag: (delta: Float) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = NextGalleryTheme.dimensions
    Box(
        modifier
            .size(dimensions.scrollbarTouchWidth, dimensions.scrollbarThumbHeight)
            .testTag(FAST_SCROLLBAR_THUMB_TAG)
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

@Composable
private fun ScrollbarLabel(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(NextGalleryTheme.dimensions.spacingSmall),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
