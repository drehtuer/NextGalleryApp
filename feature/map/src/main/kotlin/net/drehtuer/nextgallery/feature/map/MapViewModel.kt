package net.drehtuer.nextgallery.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.index.IndexRepository
import net.drehtuer.nextgallery.core.data.index.IndexScheduler
import net.drehtuer.nextgallery.core.data.index.IndexStatus
import net.drehtuer.nextgallery.core.data.map.LocationProvider
import net.drehtuer.nextgallery.core.data.map.MapPermissions
import net.drehtuer.nextgallery.core.data.map.MapRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.model.MapCluster
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import net.drehtuer.nextgallery.core.model.UserSettings
import net.drehtuer.nextgallery.core.model.Viewport
import javax.inject.Inject

/**
 * [camera] is where a new map view has to start, e.g. after coming back from another tab.
 * [openCluster] is the cluster whose bottom sheet is showing.
 * [selectedIds] is the app-wide selection, so it can contain images without a location (D33).
 */
data class MapUiState(
    val clusters: List<MapCluster> = emptyList(),
    val camera: MapCamera? = null,
    val currentLocation: GeoPoint? = null,
    val openCluster: MapCluster? = null,
    val showPhotoLocationCard: Boolean = false,
    val showNoGeotaggedCard: Boolean = false,
    val tileUrl: String = UserSettings.DEFAULT_TILE_URL,
    val selectedIds: Set<MediaId> = emptySet(),
)

sealed interface MapEvent {
    data class ViewportChanged(val viewport: Viewport) : MapEvent

    data class ClusterClicked(val key: Long) : MapEvent

    data object ClusterClosed : MapEvent

    data object ZoomIn : MapEvent

    data object ZoomOut : MapEvent

    data object ShowMyLocation : MapEvent

    data object AllowPhotoLocations : MapEvent

    data object DismissPhotoLocations : MapEvent

    /** The app came back to the foreground or the photo location prompt closed. */
    data object PermissionsChanged : MapEvent

    /** Android's location prompt closed. */
    data object CurrentLocationPromptClosed : MapEvent
}

/** What the images in the bottom sheet and the selection bar can do. */
sealed interface MapSelectionEvent : MapEvent {
    data class ItemClicked(val id: MediaId) : MapSelectionEvent

    data class ItemLongPressed(val id: MediaId) : MapSelectionEvent

    data object Clear : MapSelectionEvent

    data object Share : MapSelectionEvent

    data object Delete : MapSelectionEvent

    data class ConfirmationResult(val confirmed: Boolean) : MapSelectionEvent

    data object UndoDelete : MapSelectionEvent
}

sealed interface MapEffect {
    data class MoveCamera(val camera: MapCamera) : MapEffect

    data object RequestPhotoLocationPermission : MapEffect

    data object RequestCurrentLocationPermission : MapEffect

    data object LocationUnavailable : MapEffect

    data class OpenViewer(val id: MediaId, val bounds: GeoBounds) : MapEffect

    data class Share(val spec: ShareSpec) : MapEffect

    data class Trash(val effect: TrashEffect) : MapEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    private val permissions: MapPermissions,
    private val locationProvider: LocationProvider,
    private val indexScheduler: IndexScheduler,
    indexRepository: IndexRepository,
    private val selection: MapSelectionActions,
) : ViewModel() {
    private val effectChannel = Channel<MapEffect>(Channel.BUFFERED)
    private val local = MutableStateFlow(LocalState(photoLocationsAllowed = permissions.photoLocationsAllowed()))
    private var showMyLocationAfterPrompt = false

    val effects: Flow<MapEffect> = effectChannel.receiveAsFlow()

    /** The images of the open cluster. */
    val sheetItems: Flow<PagingData<MediaItem>> = local
        .map { it.openCluster?.cluster?.bounds }
        .distinctUntilChanged()
        .flatMapLatest { bounds ->
            if (bounds == null) flowOf(PagingData.empty()) else mediaRepository.pagedItems(MediaSource.Map(bounds))
        }
        .cachedIn(viewModelScope)

    /** While indexing, images may still get their location, so "no geotagged images" would be premature. */
    private val noGeotagged: Flow<Boolean> = combine(
        mapRepository.observeHasGeotagged(),
        indexRepository.status,
    ) { hasGeotagged, status -> !hasGeotagged && status != IndexStatus.Running }

    val state: StateFlow<MapUiState> = combine(
        mapRepository.observeClusters(local.map { it.viewport }.distinctUntilChanged().filterNotNull()),
        noGeotagged,
        selection.observe(),
        settingsRepository.observeSettings(),
        local,
    ) { clusters, noGeotagged, selectedIds, settings, local ->
        MapUiState(
            clusters = clusters,
            camera = local.camera,
            currentLocation = local.currentLocation,
            openCluster = local.openCluster,
            showPhotoLocationCard = !local.photoLocationsAllowed && !local.photoLocationCardDismissed,
            showNoGeotaggedCard = local.photoLocationsAllowed && noGeotagged,
            tileUrl = settings.tileUrl,
            selectedIds = selectedIds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MapUiState())

    init {
        viewModelScope.launch { open() }
    }

    fun onEvent(event: MapEvent) {
        when (event) {
            is MapSelectionEvent -> viewModelScope.launch {
                selection.handle(event, local.value.openCluster?.cluster?.bounds)?.let { effectChannel.send(it) }
            }

            is MapEvent.ViewportChanged -> local.update {
                it.copy(viewport = event.viewport, camera = MapCamera(event.viewport.center, event.viewport.zoom))
            }

            is MapEvent.ClusterClicked -> local.update { current ->
                current.copy(openCluster = state.value.clusters.find { it.cluster.coverId.value == event.key })
            }

            MapEvent.ClusterClosed -> local.update { it.copy(openCluster = null) }

            MapEvent.ZoomIn -> zoomBy(1.0)

            MapEvent.ZoomOut -> zoomBy(-1.0)

            MapEvent.ShowMyLocation -> onShowMyLocation()

            MapEvent.AllowPhotoLocations -> effectChannel.trySend(MapEffect.RequestPhotoLocationPermission)

            MapEvent.DismissPhotoLocations -> local.update { it.copy(photoLocationCardDismissed = true) }

            MapEvent.PermissionsChanged -> refreshPhotoLocations()

            MapEvent.CurrentLocationPromptClosed -> onCurrentLocationPromptClosed()
        }
    }

    /** Android's location prompt shows once (D44); after that the map opens without asking again. */
    private suspend fun open() {
        val asked = settingsRepository.observeSettings().first().currentLocationAsked
        if (!permissions.currentLocationAllowed() && !asked) {
            settingsRepository.setCurrentLocationAsked()
            effectChannel.send(MapEffect.RequestCurrentLocationPermission)
        } else {
            moveToStart()
        }
    }

    private suspend fun moveToStart() {
        val location = currentLocationIfAllowed()
        local.update { it.copy(currentLocation = location) }
        moveCamera(mapRepository.startCamera(location))
    }

    private suspend fun moveToCurrentLocation() {
        val location = currentLocationIfAllowed()
        local.update { it.copy(currentLocation = location) }
        if (location == null) {
            effectChannel.send(MapEffect.LocationUnavailable)
        } else {
            moveCamera(MapCamera(location, START_ZOOM))
        }
    }

    private suspend fun currentLocationIfAllowed(): GeoPoint? =
        if (permissions.currentLocationAllowed()) locationProvider.currentLocation() else null

    private fun moveCamera(camera: MapCamera) {
        local.update { it.copy(camera = camera) }
        effectChannel.trySend(MapEffect.MoveCamera(camera))
    }

    private fun zoomBy(delta: Double) {
        val viewport = local.value.viewport ?: return
        moveCamera(MapCamera(viewport.center, (viewport.zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)))
    }

    private fun onShowMyLocation() {
        if (permissions.currentLocationAllowed()) {
            viewModelScope.launch { moveToCurrentLocation() }
        } else {
            showMyLocationAfterPrompt = true
            effectChannel.trySend(MapEffect.RequestCurrentLocationPermission)
        }
    }

    private fun onCurrentLocationPromptClosed() {
        val showMyLocation = showMyLocationAfterPrompt
        showMyLocationAfterPrompt = false
        viewModelScope.launch { if (showMyLocation) moveToCurrentLocation() else moveToStart() }
    }

    private fun refreshPhotoLocations() {
        val allowed = permissions.photoLocationsAllowed()
        if (allowed && !local.value.photoLocationsAllowed) indexScheduler.onPhotoLocationsAllowed()
        local.update { it.copy(photoLocationsAllowed = allowed) }
    }

    private data class LocalState(
        val photoLocationsAllowed: Boolean,
        val viewport: Viewport? = null,
        val camera: MapCamera? = null,
        val currentLocation: GeoPoint? = null,
        val openCluster: MapCluster? = null,
        val photoLocationCardDismissed: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MIN_ZOOM = 0.0
        const val MAX_ZOOM = 19.0
        const val START_ZOOM = 8.0
    }
}
