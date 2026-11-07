package net.drehtuer.nextgallery.core.data.index

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem

/** Port: the locations in the local image index. Implemented with Room in `:core:database`. */
interface GeoIndexStore {
    /** All images with a location, newest first. */
    fun observeGeotagged(): Flow<List<GeotaggedImage>>

    /** Images whose location has not been looked for yet, at most [limit]. */
    suspend fun unscanned(limit: Int): List<MediaItem>

    /** Stores what the location pass found; null means that the image has no location. */
    suspend fun saveLocations(locations: Map<MediaId, GeoPoint?>)

    /** Images inside [bounds], newest first. [bounds] must not cross the antimeridian. */
    fun pagedInBounds(bounds: GeoBounds, config: PagingConfig): Flow<PagingData<MediaItem>>

    /** Position of an image in [pagedInBounds]. */
    suspend fun indexInBounds(bounds: GeoBounds, id: MediaId): Int
}
