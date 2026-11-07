package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import net.drehtuer.nextgallery.core.model.MediaId

internal const val VISIBLE_IDS_DEBOUNCE_MILLIS = 150L

/**
 * Reports the images on screen, for the upload badges. A fast scroll causes no query until it slows down, so the
 * paging query and the scrolling are unaffected. Grid cells of images use the media id as their key; other cells
 * (headers, items not loaded yet) have other keys and are left out.
 */
@OptIn(FlowPreview::class)
@Composable
fun VisibleMediaIdsEffect(state: LazyGridState, onChanged: (Set<MediaId>) -> Unit) {
    val currentOnChanged by rememberUpdatedState(onChanged)
    LaunchedEffect(state) {
        snapshotFlow {
            state.layoutInfo.visibleItemsInfo.mapNotNullTo(HashSet()) { cell -> (cell.key as? Long)?.let(::MediaId) }
        }
            .distinctUntilChanged()
            .debounce(VISIBLE_IDS_DEBOUNCE_MILLIS)
            .collect { currentOnChanged(it) }
    }
}
