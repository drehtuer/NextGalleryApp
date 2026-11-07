package net.drehtuer.nextgallery.feature.map

import net.drehtuer.nextgallery.core.model.Cluster
import net.drehtuer.nextgallery.core.model.ClusterTitle
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MapCamera
import net.drehtuer.nextgallery.core.model.MapCluster
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.Place
import net.drehtuer.nextgallery.core.model.Viewport
import java.time.LocalDate

val bolzano = Place("Bolzano", "IT", 46.49, 11.33, 107_436)

fun mediaItem(id: Long) = MediaItem(
    id = MediaId(id),
    volumeName = "external_primary",
    contentUri = "content://media/$id",
    bucketId = 7,
    bucketName = "Camera",
    relativePath = "DCIM/Camera/",
    displayName = "IMG_$id.jpg",
    mimeType = "image/jpeg",
    sizeBytes = 1,
    width = 1,
    height = 1,
    orientation = 0,
    dateTakenMillis = id,
    dateSource = DateSource.DATE_TAKEN,
    takenLocalDate = LocalDate.of(2024, 3, 15),
    dateModifiedSeconds = 1,
    generationModified = 1,
    location = GeoPoint(46.5, 11.3),
)

/** A cluster of [count] images, whose cover has the id [count]. */
fun mapCluster(count: Int = 1, title: ClusterTitle? = ClusterTitle(bolzano, 0)): MapCluster {
    val center = GeoPoint(46.5, 11.3)
    val items = (1..count).map { GeotaggedImage(MediaId(it.toLong()), center, dateTakenMillis = it.toLong()) }
    return MapCluster(Cluster(center, items), title, mediaItem(count.toLong()))
}

val testViewport = Viewport(
    center = GeoPoint(46.5, 11.3),
    zoom = 10.0,
    bounds = GeoBounds(south = 46.0, west = 11.0, north = 47.0, east = 12.0),
)

class FakeMapRenderer : MapRenderer {
    override var onViewportChanged: ((Viewport) -> Unit)? = null
    override var onClusterClick: ((key: Long) -> Unit)? = null
    var markers: List<MapMarker> = emptyList()
    var currentCamera: MapCamera? = null
    var location: GeoPoint? = null
    var currentTileUrl: String? = null

    override fun setClusters(markers: List<MapMarker>) {
        this.markers = markers
    }

    override fun setCamera(camera: MapCamera) {
        currentCamera = camera
    }

    override fun setCurrentLocation(location: GeoPoint?) {
        this.location = location
    }

    override fun setTileUrl(url: String) {
        currentTileUrl = url
    }
}
