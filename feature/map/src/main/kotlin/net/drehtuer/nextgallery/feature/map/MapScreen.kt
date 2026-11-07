package net.drehtuer.nextgallery.feature.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.ui.component.DeleteAction
import net.drehtuer.nextgallery.core.ui.component.SelectionOverviewAction
import net.drehtuer.nextgallery.core.ui.component.SelectionTopBar
import net.drehtuer.nextgallery.core.ui.component.ShareAction
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

/** The map itself comes through [map], because the real one needs a device (ARCHITECTURE.md §13.1). */
@Composable
fun MapScreen(
    state: MapUiState,
    sheetItems: LazyPagingItems<MediaItem>,
    onEvent: (MapEvent) -> Unit,
    onOpenSelectionOverview: () -> Unit,
    map: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val selecting = state.selectedIds.isNotEmpty()
    BackHandler(enabled = selecting) { onEvent(MapSelectionEvent.Clear) }
    Box(modifier.fillMaxSize()) {
        map(Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            if (selecting) MapSelectionBar(state.selectedIds, onEvent, onOpenSelectionOverview)
            if (state.showPhotoLocationCard) PhotoLocationCard(onEvent)
            if (state.showNoGeotaggedCard) NoGeotaggedCard()
        }
        MapControls(onEvent, Modifier.align(Alignment.BottomEnd))
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
    state.openCluster?.let { cluster ->
        ClusterSheet(cluster, sheetItems, state.selectedIds, onEvent, onOpenSelectionOverview)
    }
}

/** The app-wide selection (D33): the actions apply to all of it, not only to what the map shows. */
@Composable
internal fun MapSelectionBar(
    selectedIds: Set<MediaId>,
    onEvent: (MapEvent) -> Unit,
    onOpenSelectionOverview: () -> Unit,
) {
    SelectionTopBar(selectionCount = selectedIds.size, onClear = { onEvent(MapSelectionEvent.Clear) }) {
        SelectionOverviewAction(onClick = onOpenSelectionOverview)
        ShareAction(onClick = { onEvent(MapSelectionEvent.Share) })
        DeleteAction(onClick = { onEvent(MapSelectionEvent.Delete) })
    }
}

@Composable
private fun PhotoLocationCard(onEvent: (MapEvent) -> Unit) {
    MapCard(
        title = stringResource(R.string.feature_map_permission_title),
        message = stringResource(R.string.feature_map_permission_message),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NextGalleryTheme.dimensions.spacingSmall, Alignment.End),
        ) {
            TextButton(onClick = { onEvent(MapEvent.DismissPhotoLocations) }) {
                Text(stringResource(R.string.feature_map_permission_not_now))
            }
            Button(onClick = { onEvent(MapEvent.AllowPhotoLocations) }) {
                Text(stringResource(R.string.feature_map_permission_allow))
            }
        }
    }
}

@Composable
private fun NoGeotaggedCard() {
    MapCard(
        title = stringResource(R.string.feature_map_no_geotagged_title),
        message = stringResource(R.string.feature_map_no_geotagged_message),
    )
}

@Composable
private fun MapCard(title: String, message: String, actions: @Composable () -> Unit = {}) {
    val dimensions = NextGalleryTheme.dimensions
    Surface(
        modifier = Modifier.fillMaxWidth().padding(dimensions.spacingMedium),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.padding(dimensions.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            actions()
        }
    }
}

/** Bottom right: zoom, my location, and the attribution in the corner below them (DESIGN.md). */
@Composable
private fun MapControls(onEvent: (MapEvent) -> Unit, modifier: Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    Column(
        modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        Column(
            Modifier.padding(horizontal = dimensions.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        ) {
            ZoomColumn(onEvent)
            MyLocationButton(onEvent)
        }
        Text(
            stringResource(R.string.feature_map_attribution),
            modifier = Modifier
                .background(NextGalleryTheme.mapColors.land)
                .padding(horizontal = dimensions.spacingSmall),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ZoomColumn(onEvent: (MapEvent) -> Unit) {
    val buttonSize = NextGalleryTheme.dimensions.mapZoomButtonSize
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            IconButton(onClick = { onEvent(MapEvent.ZoomIn) }, modifier = Modifier.size(buttonSize)) {
                Icon(
                    painterResource(R.drawable.feature_map_ic_zoom_in),
                    stringResource(R.string.feature_map_zoom_in),
                )
            }
            IconButton(onClick = { onEvent(MapEvent.ZoomOut) }, modifier = Modifier.size(buttonSize)) {
                Icon(
                    painterResource(R.drawable.feature_map_ic_zoom_out),
                    stringResource(R.string.feature_map_zoom_out),
                )
            }
        }
    }
}

@Composable
private fun MyLocationButton(onEvent: (MapEvent) -> Unit) {
    Surface(
        onClick = { onEvent(MapEvent.ShowMyLocation) },
        modifier = Modifier.size(NextGalleryTheme.dimensions.mapLocationButtonSize),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.feature_map_ic_my_location),
                stringResource(R.string.feature_map_my_location),
            )
        }
    }
}
