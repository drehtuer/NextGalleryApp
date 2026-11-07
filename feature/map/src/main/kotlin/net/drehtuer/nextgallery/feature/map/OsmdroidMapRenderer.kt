package net.drehtuer.nextgallery.feature.map

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.model.Viewport
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint as OsmGeoPoint

/** [MapRenderer] on osmdroid (D23). Everything it decides is in [OsmdroidSetup] and `MarkerStyle.kt`. */
internal class OsmdroidMapRenderer(
    private val context: Context,
    private val style: MarkerStyle,
    private val imageLoader: ImageLoader = SingletonImageLoader.get(context),
) : MapRenderer {
    override var onViewportChanged: ((Viewport) -> Unit)? = null
    override var onClusterClick: ((key: Long) -> Unit)? = null

    val view: MapView
    private val shown = HashMap<MapMarker, ShownMarker>()
    private var locationMarker: Marker? = null
    private var tileUrl: String? = null

    init {
        // The configuration is global, and osmdroid reads it when a map view is created.
        OsmdroidSetup.configure(Configuration.getInstance(), context.cacheDir)
        view = MapView(context)
        OsmdroidSetup.configure(view)
        view.addMapListener(
            object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean = reportViewport()

                override fun onZoom(event: ZoomEvent?): Boolean = reportViewport()
            },
        )
        view.addOnFirstLayoutListener { _, _, _, _, _ -> reportViewport() }
    }

    override fun setClusters(markers: List<MapMarker>) {
        val wanted = markers.toSet()
        if (wanted == shown.keys) return
        (shown.keys - wanted).forEach { marker ->
            shown.remove(marker)?.let {
                it.loading?.dispose()
                view.overlays.remove(it.overlay)
            }
        }
        (wanted - shown.keys).forEach { shown[it] = show(it) }
        view.invalidate()
    }

    override fun setCamera(camera: MapCamera) {
        view.controller.setZoom(camera.zoom)
        view.controller.setCenter(OsmGeoPoint(camera.center.latitude, camera.center.longitude))
    }

    override fun setCurrentLocation(location: GeoPoint?) {
        locationMarker?.let(view.overlays::remove)
        locationMarker = location?.let { point ->
            newMarker(point, drawLocationDot(style)).also { view.overlays.add(it) }
        }
        view.invalidate()
    }

    override fun setTileUrl(url: String) {
        if (url == tileUrl) return
        tileUrl = url
        view.setTileSource(TemplateTileSource(url))
    }

    private fun show(marker: MapMarker): ShownMarker {
        val overlay = newMarker(marker.position, drawClusterMarker(style, null, marker.countLabel))
        overlay.title = marker.description
        overlay.setOnMarkerClickListener { _, _ ->
            onClusterClick?.invoke(marker.key)
            true
        }
        view.overlays.add(overlay)
        return ShownMarker(overlay, marker.cover?.let { loadCover(it, overlay, marker.countLabel) })
    }

    private fun loadCover(cover: Any, overlay: Marker, countLabel: String): Disposable {
        val request = ImageRequest.Builder(context)
            .data(cover)
            .size(style.markerSizePx)
            .allowHardware(false)
            .target(
                onSuccess = { image ->
                    overlay.icon = bitmapDrawable(drawClusterMarker(style, image.toBitmap(), countLabel))
                    view.invalidate()
                },
            )
            .build()
        return imageLoader.enqueue(request)
    }

    private fun newMarker(point: GeoPoint, icon: Bitmap) = Marker(view).apply {
        position = OsmGeoPoint(point.latitude, point.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        setInfoWindow(null)
        this.icon = bitmapDrawable(icon)
    }

    private fun bitmapDrawable(bitmap: Bitmap) = bitmap.toDrawable(context.resources)

    /** Always returns false, so that other listeners also see the event. */
    private fun reportViewport(): Boolean {
        val box = view.boundingBox
        onViewportChanged?.invoke(
            viewportOf(
                centerLatitude = view.mapCenter.latitude,
                centerLongitude = view.mapCenter.longitude,
                zoom = view.zoomLevelDouble,
                bounds = GeoBounds(box.latSouth, box.lonWest, box.latNorth, box.lonEast),
            ),
        )
        return false
    }

    private class ShownMarker(val overlay: Marker, val loading: Disposable?)
}

/** osmdroid can report positions just outside the valid range when the world repeats. */
internal fun viewportOf(centerLatitude: Double, centerLongitude: Double, zoom: Double, bounds: GeoBounds) = Viewport(
    center = GeoPoint(centerLatitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE), wrapLongitude(centerLongitude)),
    zoom = zoom,
    bounds = GeoBounds(
        south = bounds.south.coerceIn(-MAX_LATITUDE, MAX_LATITUDE),
        west = wrapLongitude(bounds.west),
        north = bounds.north.coerceIn(-MAX_LATITUDE, MAX_LATITUDE),
        east = wrapLongitude(bounds.east),
    ),
)

private fun wrapLongitude(longitude: Double): Double = if (longitude in -HALF_CIRCLE..HALF_CIRCLE) {
    longitude
} else {
    (longitude + HALF_CIRCLE).mod(2 * HALF_CIRCLE) - HALF_CIRCLE
}

private const val MAX_LATITUDE = 90.0
private const val HALF_CIRCLE = 180.0
