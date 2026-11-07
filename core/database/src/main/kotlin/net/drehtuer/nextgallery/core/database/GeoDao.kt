package net.drehtuer.nextgallery.core.database

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** A map point: only what clustering needs, so that large libraries stay cheap. */
data class GeotaggedRow(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "date_taken_ms") val dateTakenMillis: Long,
)

/** A location to store; both values are null if the image has none. */
data class LocationRow(val id: Long, val latitude: Double?, val longitude: Double?)

/** The location side of the `media` table. Reads and writes rows, so it needs no table of its own. */
@Dao
interface GeoDao {
    @Query(
        """
        SELECT id, latitude, longitude, date_taken_ms FROM media
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        ORDER BY date_taken_ms DESC, id DESC
        """,
    )
    fun observeGeotagged(): Flow<List<GeotaggedRow>>

    @Query("SELECT * FROM media WHERE exif_scanned = 0 LIMIT :limit")
    suspend fun unscanned(limit: Int): List<MediaEntity>

    @Query("UPDATE media SET latitude = :latitude, longitude = :longitude, exif_scanned = 1 WHERE id = :id")
    suspend fun setLocation(id: Long, latitude: Double?, longitude: Double?)

    @Transaction
    suspend fun setLocations(rows: List<LocationRow>) {
        rows.forEach { setLocation(it.id, it.latitude, it.longitude) }
    }

    @Query(
        """
        SELECT * FROM media
        WHERE latitude BETWEEN :south AND :north AND longitude BETWEEN :west AND :east
        ORDER BY date_taken_ms DESC, id DESC
        """,
    )
    fun pagingInBounds(south: Double, west: Double, north: Double, east: Double): PagingSource<Int, MediaEntity>

    /** The number of images before this one in [pagingInBounds]. */
    @Query(
        """
        SELECT COUNT(*) FROM media
        JOIN media AS target ON target.id = :id
        WHERE media.latitude BETWEEN :south AND :north AND media.longitude BETWEEN :west AND :east
            AND (
                media.date_taken_ms > target.date_taken_ms
                OR (media.date_taken_ms = target.date_taken_ms AND media.id > target.id)
            )
        """,
    )
    suspend fun indexInBounds(south: Double, west: Double, north: Double, east: Double, id: Long): Int
}
