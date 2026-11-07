package net.drehtuer.nextgallery.core.data.map

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.index.GeoIndexStore
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.model.MapCluster
import net.drehtuer.nextgallery.core.model.Viewport
import javax.inject.Inject
import kotlin.math.roundToInt

interface MapRepository {
    /** The clusters in the viewport, recomputed when it settles or when the index changes. */
    fun observeClusters(viewport: Flow<Viewport>): Flow<List<MapCluster>>

    fun observeHasGeotagged(): Flow<Boolean>

    /** Where the map opens (D44): at [location] if it is known, otherwise on the area with the most images. */
    suspend fun startCamera(location: GeoPoint?): MapCamera
}

class DefaultMapRepository @Inject constructor(
    private val geoStore: GeoIndexStore,
    private val indexStore: MediaIndexStore,
    private val placeNamer: PlaceNamer,
    private val dispatchers: DispatcherProvider,
) : MapRepository {
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun observeClusters(viewport: Flow<Viewport>): Flow<List<MapCluster>> = combine(
        geoStore.observeGeotagged(),
        viewport.debounce(VIEWPORT_DEBOUNCE_MILLIS),
    ) { images, current -> images to current }
        .mapLatest { (images, current) -> withContext(dispatchers.default) { clusters(images, current) } }

    override fun observeHasGeotagged(): Flow<Boolean> =
        geoStore.observeGeotagged().map { it.isNotEmpty() }.distinctUntilChanged()

    override suspend fun startCamera(location: GeoPoint?): MapCamera {
        if (location != null) return MapCamera(location, START_ZOOM)
        val images = geoStore.observeGeotagged().first()
        val largest = withContext(dispatchers.default) {
            GridClusterer.cluster(images, WORLD, AREA_ZOOM, AREA_CELL_PX).maxByOrNull { it.count }
        }
        return if (largest != null) MapCamera(largest.center, START_ZOOM) else MapCamera(WORLD_CENTER, WORLD_ZOOM)
    }

    private suspend fun clusters(images: List<GeotaggedImage>, viewport: Viewport): List<MapCluster> {
        val clusters = GridClusterer.cluster(images, viewport.bounds, viewport.zoom.roundToInt(), CELL_PX)
        val covers = indexStore.getByIds(clusters.map { it.coverId }).associateBy { it.id }
        return clusters.map { MapCluster(it, placeNamer.clusterTitle(it), covers[it.coverId]) }
    }

    internal companion object {
        const val VIEWPORT_DEBOUNCE_MILLIS = 150L

        /** A little larger than a marker in dp, so markers do not cover each other. */
        const val CELL_PX = 80
        const val START_ZOOM = 8.0

        /** At this zoom one cell is about a phone screen at [START_ZOOM]. */
        const val AREA_ZOOM = 7
        const val AREA_CELL_PX = 256
        const val WORLD_ZOOM = 2.0
        val WORLD_CENTER = GeoPoint(0.0, 0.0)
        val WORLD = GeoBounds(south = -90.0, west = -180.0, north = 90.0, east = 180.0)
    }
}
