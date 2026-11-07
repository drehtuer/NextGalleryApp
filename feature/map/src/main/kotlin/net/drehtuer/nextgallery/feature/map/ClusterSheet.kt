package net.drehtuer.nextgallery.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.LazyPagingItems
import net.drehtuer.nextgallery.core.model.MapCluster
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.ui.component.FastScrollbar
import net.drehtuer.nextgallery.core.ui.component.MediaGrid
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClusterSheet(
    cluster: MapCluster,
    items: LazyPagingItems<MediaItem>,
    selectedIds: Set<MediaId>,
    onEvent: (MapEvent) -> Unit,
    onOpenSelectionOverview: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(MapEvent.ClusterClosed) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ClusterSheetContent(cluster, items, selectedIds, onEvent, onOpenSelectionOverview)
    }
}

@Composable
internal fun ClusterSheetContent(
    cluster: MapCluster,
    items: LazyPagingItems<MediaItem>,
    selectedIds: Set<MediaId>,
    onEvent: (MapEvent) -> Unit,
    onOpenSelectionOverview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = NextGalleryTheme.dimensions
    val resources = LocalResources.current
    val gridState = rememberLazyGridState()
    Column(modifier.fillMaxWidth().height(dimensions.clusterSheetHeight)) {
        if (selectedIds.isNotEmpty()) {
            Box(Modifier.consumeWindowInsets(WindowInsets.statusBars)) {
                MapSelectionBar(selectedIds, onEvent, onOpenSelectionOverview)
            }
        } else {
            Column(
                Modifier.padding(horizontal = dimensions.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
            ) {
                Text(
                    clusterSheetTitle(resources, cluster),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    clusterSheetSubtitle(resources, cluster),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(Modifier.padding(top = dimensions.spacingSmall).weight(1f)) {
            MediaGrid(
                items = items,
                selectedIds = selectedIds,
                onItemClick = { onEvent(MapSelectionEvent.ItemClicked(it.id)) },
                onItemLongClick = { onEvent(MapSelectionEvent.ItemLongPressed(it.id)) },
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = dimensions.clusterSheetColumns,
            )
            FastScrollbar(gridState, items.itemCount, Modifier.align(Alignment.TopEnd))
        }
    }
}
