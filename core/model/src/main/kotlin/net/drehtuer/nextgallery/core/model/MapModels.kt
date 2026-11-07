package net.drehtuer.nextgallery.core.model

/** What the map should show. */
data class MapCamera(val center: GeoPoint, val zoom: Double)

/** What the map shows right now. */
data class Viewport(val center: GeoPoint, val zoom: Double, val bounds: GeoBounds)

/** The lightweight form of an image for clustering, so that 50,000 images fit in memory. */
data class GeotaggedImage(val id: MediaId, val location: GeoPoint, val dateTakenMillis: Long)

/** Images that are close together at the current zoom. [items] is never empty. */
data class Cluster(val center: GeoPoint, val items: List<GeotaggedImage>) {
    val count: Int
        get() = items.size

    /** The cover is the newest image. */
    val coverId: MediaId
        get() = items.maxBy { it.dateTakenMillis }.id

    /** Contains exactly this cluster's images, because a cluster is everything in one rectangular grid cell. */
    val bounds: GeoBounds
        get() = GeoBounds.around(items.map { it.location })
}

/** A place from the bundled GeoNames list (D45). */
data class Place(
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val population: Long,
)

/** A cluster's name: the largest place near its images, and how many other places are near them (D45). */
data class ClusterTitle(val place: Place, val morePlaces: Int)

/** A cluster as the map shows it. [title] is null without a town; [cover] is null if the image has just gone. */
data class MapCluster(val cluster: Cluster, val title: ClusterTitle?, val cover: MediaItem?)
