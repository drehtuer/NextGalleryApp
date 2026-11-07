package net.drehtuer.nextgallery.feature.map

import android.content.res.Resources
import net.drehtuer.nextgallery.core.model.MapCluster
import net.drehtuer.nextgallery.core.ui.thumbnail.toThumbnail

private const val MAX_EXACT_COUNT = 999

/** The count on a marker (DESIGN.md). */
internal fun clusterCountLabel(count: Int): String = if (count > MAX_EXACT_COUNT) "$MAX_EXACT_COUNT+" else "$count"

/** "near Bolzano, 128 images", or "4 images" without a town. */
internal fun clusterDescription(resources: Resources, cluster: MapCluster): String {
    val count = cluster.cluster.count
    val place = cluster.title?.place ?: return imageCount(resources, count)
    return resources.getQuantityString(R.plurals.feature_map_marker_description, count, place.name, count)
}

/** "Near Bolzano", "Near Bolzano and 7 more places", or "4 images" without a town. */
internal fun clusterSheetTitle(resources: Resources, cluster: MapCluster): String {
    val title = cluster.title ?: return imageCount(resources, cluster.cluster.count)
    return if (title.morePlaces == 0) {
        resources.getString(R.string.feature_map_near_title, title.place.name)
    } else {
        resources.getQuantityString(
            R.plurals.feature_map_near_more_title,
            title.morePlaces,
            title.place.name,
            title.morePlaces,
        )
    }
}

/** The line under the title: the image count, or the coordinates when the title already is the count. */
internal fun clusterSheetSubtitle(resources: Resources, cluster: MapCluster): String = if (cluster.title == null) {
    cluster.cluster.center.formatted()
} else {
    imageCount(resources, cluster.cluster.count)
}

internal fun MapCluster.toMarker(resources: Resources) = MapMarker(
    key = cluster.coverId.value,
    position = cluster.center,
    countLabel = clusterCountLabel(cluster.count),
    cover = cover?.toThumbnail(),
    description = clusterDescription(resources, this),
)

private fun imageCount(resources: Resources, count: Int): String =
    resources.getQuantityString(R.plurals.feature_map_image_count, count, count)
