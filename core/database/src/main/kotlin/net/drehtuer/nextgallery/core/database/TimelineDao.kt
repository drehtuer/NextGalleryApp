package net.drehtuer.nextgallery.core.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A result row of [TimelineDao.observeDayCounts]. */
data class DayCountRow(val year: Int, val month: Int, val day: Int, val count: Int)

@Dao
interface TimelineDao {
    /** By local date first, so that the images of a day stay together even if the time zone changed between them. */
    @Query(
        "SELECT * FROM media ORDER BY local_year DESC, local_month DESC, local_day DESC, date_taken_ms DESC, id DESC",
    )
    fun paging(): PagingSource<Int, MediaEntity>

    @Query(
        "SELECT id FROM media ORDER BY local_year DESC, local_month DESC, local_day DESC, date_taken_ms DESC, id DESC",
    )
    suspend fun ids(): List<Long>

    /** The number of images before this one in [paging]. */
    @Query(
        """
        SELECT COUNT(*) FROM media
        JOIN media AS target ON target.id = :id
        WHERE (media.local_year, media.local_month, media.local_day, media.date_taken_ms, media.id)
            > (target.local_year, target.local_month, target.local_day, target.date_taken_ms, target.id)
        """,
    )
    suspend fun indexOf(id: Long): Int

    @Query(
        """
        SELECT local_year AS year, local_month AS month, local_day AS day, COUNT(*) AS count FROM media
        GROUP BY local_year, local_month, local_day
        ORDER BY local_year DESC, local_month DESC, local_day DESC
        """,
    )
    fun observeDayCounts(): Flow<List<DayCountRow>>
}
