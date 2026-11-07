package net.drehtuer.nextgallery.core.data.index

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.drehtuer.nextgallery.core.data.map.MapPermissions
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import javax.inject.Inject

/**
 * The second indexing pass (ARCHITECTURE.md §5.1): reads the GPS tags of images it has not looked at yet, in batches,
 * so that an interrupted run continues where it stopped. Without `ACCESS_MEDIA_LOCATION` Android hides the location
 * in the file, so it does nothing and the images stay unscanned until the permission is granted.
 */
class LocationBackfill @Inject constructor(
    private val geoStore: GeoIndexStore,
    private val exifSource: ExifSource,
    private val permissions: MapPermissions,
) {
    /** Returns the number of images it looked at. */
    suspend fun run(): Int {
        if (!permissions.photoLocationsAllowed()) return 0
        var scanned = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val batch = geoStore.unscanned(BATCH_SIZE)
            if (batch.isEmpty()) return scanned
            val locations = HashMap<MediaId, GeoPoint?>()
            batch.forEach { locations[it.id] = locationOf(it) }
            geoStore.saveLocations(locations)
            scanned += batch.size
        }
    }

    private suspend fun locationOf(item: MediaItem): GeoPoint? = if (item.mimeType in MIME_TYPES_WITH_GPS) {
        exifSource.read(item.contentUri)?.let(GpsParser::parse)
    } else {
        null
    }

    private companion object {
        const val BATCH_SIZE = 200
        val MIME_TYPES_WITH_GPS = setOf("image/jpeg", "image/heic", "image/heif", "image/webp")
    }
}
