package net.drehtuer.nextgallery.core.database

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.data.index.GeoIndexStore
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import javax.inject.Inject

class RoomGeoIndexStore @Inject constructor(private val geoDao: GeoDao) : GeoIndexStore {
    override fun observeGeotagged(): Flow<List<GeotaggedImage>> = geoDao.observeGeotagged().map { rows ->
        rows.map { GeotaggedImage(MediaId(it.id), GeoPoint(it.latitude, it.longitude), it.dateTakenMillis) }
    }

    override suspend fun unscanned(limit: Int): List<MediaItem> = geoDao.unscanned(limit).map { it.toModel() }

    override suspend fun saveLocations(locations: Map<MediaId, GeoPoint?>) = geoDao.setLocations(
        locations.map { (id, location) -> LocationRow(id.value, location?.latitude, location?.longitude) },
    )

    override fun pagedInBounds(bounds: GeoBounds, config: PagingConfig): Flow<PagingData<MediaItem>> = Pager(config) {
        geoDao.pagingInBounds(bounds.south, bounds.west, bounds.north, bounds.east)
    }.flow.map { page -> page.map { it.toModel() } }

    override suspend fun indexInBounds(bounds: GeoBounds, id: MediaId): Int =
        geoDao.indexInBounds(bounds.south, bounds.west, bounds.north, bounds.east, id.value)
}
