package net.drehtuer.nextgallery.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources

/** Keeps a [MapRenderer] in line with the state, and passes on what the user does on the map. */
@Composable
internal fun MapRendererBinding(renderer: MapRenderer, state: MapUiState, onEvent: (MapEvent) -> Unit) {
    val resources = LocalResources.current
    val currentOnEvent = rememberUpdatedState(onEvent)
    // Not in a coroutine: this has to be in place before the first layout, which reports the first viewport.
    DisposableEffect(renderer) {
        renderer.onViewportChanged = { currentOnEvent.value(MapEvent.ViewportChanged(it)) }
        renderer.onClusterClick = { currentOnEvent.value(MapEvent.ClusterClicked(it)) }
        // A new map view, e.g. after coming back from another tab, starts where the last one was.
        state.camera?.let(renderer::setCamera)
        onDispose {
            renderer.onViewportChanged = null
            renderer.onClusterClick = null
        }
    }
    LaunchedEffect(renderer, state.tileUrl) { renderer.setTileUrl(state.tileUrl) }
    LaunchedEffect(renderer, state.currentLocation) { renderer.setCurrentLocation(state.currentLocation) }
    SideEffect { renderer.setClusters(state.clusters.map { it.toMarker(resources) }) }
}
