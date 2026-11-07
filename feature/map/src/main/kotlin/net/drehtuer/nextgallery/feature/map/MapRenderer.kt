package net.drehtuer.nextgallery.feature.map

import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.model.Viewport
import net.drehtuer.nextgallery.core.ui.thumbnail.MediaThumbnail

/** One cluster as the map library draws it. [key] comes back through [MapRenderer.onClusterClick]. */
data class MapMarker(
    val key: Long,
    val position: GeoPoint,
    val countLabel: String,
    val cover: MediaThumbnail?,
    val description: String,
)

/** What the app needs from a map library (D23), so that osmdroid can be replaced without touching the rest. */
interface MapRenderer {
    var onViewportChanged: ((Viewport) -> Unit)?
    var onClusterClick: ((key: Long) -> Unit)?

    fun setClusters(markers: List<MapMarker>)

    fun setCamera(camera: MapCamera)

    /** Draws the location dot; null hides it. */
    fun setCurrentLocation(location: GeoPoint?)

    fun setTileUrl(url: String)
}
