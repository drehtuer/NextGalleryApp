package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.thumbnail.ThumbnailCell
import net.drehtuer.nextgallery.core.ui.thumbnail.thumbnailSizePx
import net.drehtuer.nextgallery.core.ui.thumbnail.toThumbnail

private const val MEDIA_CONTENT_TYPE = "media"

/** Paged thumbnail grid. Items that are not loaded yet show a placeholder and do not react to clicks. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: LazyPagingItems<MediaItem>,
    selectedIds: Set<MediaId>,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
) {
    val dimensions = NextGalleryTheme.dimensions
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val sizePx = with(density) {
            thumbnailSizePx(maxWidth.roundToPx(), dimensions.mediaCellMinSize.roundToPx())
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(dimensions.mediaCellMinSize),
            modifier = Modifier.fillMaxSize(),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(dimensions.gridGap),
            verticalArrangement = Arrangement.spacedBy(dimensions.gridGap),
        ) {
            items(
                count = items.itemCount,
                key = items.itemKey { it.id.value },
                contentType = { MEDIA_CONTENT_TYPE },
            ) { index ->
                val item = items[index]
                ThumbnailCell(
                    thumbnail = item?.toThumbnail(),
                    contentDescription = item?.displayName,
                    sizePx = sizePx,
                    selected = item?.id in selectedIds,
                    modifier = Modifier.combinedClickable(
                        enabled = item != null,
                        onClick = { item?.let(onItemClick) },
                        onLongClick = { item?.let(onItemLongClick) },
                    ),
                )
            }
        }
    }
}
