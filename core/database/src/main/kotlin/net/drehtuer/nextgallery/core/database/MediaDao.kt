package net.drehtuer.nextgallery.core.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MediaDao {
    @Upsert
    suspend fun upsertAll(items: List<MediaEntity>)

    @Query("DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT id FROM media WHERE volume_name = :volume")
    suspend fun idsForVolume(volume: String): List<Long>

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getById(id: Long): MediaEntity?

    @Query("SELECT * FROM media WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<MediaEntity>

    /** The number of images before this one in [pagingByBucket]. */
    @Query(
        """
        SELECT COUNT(*) FROM media
        JOIN media AS target ON target.id = :id
        WHERE media.bucket_id = :bucketId
            AND (
                media.date_taken_ms > target.date_taken_ms
                OR (media.date_taken_ms = target.date_taken_ms AND media.id > target.id)
            )
        """,
    )
    suspend fun indexInBucket(bucketId: Long, id: Long): Int

    @Query("SELECT * FROM media WHERE bucket_id = :bucketId ORDER BY date_taken_ms DESC, id DESC")
    fun pagingByBucket(bucketId: Long): PagingSource<Int, MediaEntity>

    @Query("SELECT id FROM media WHERE bucket_id = :bucketId ORDER BY date_taken_ms DESC, id DESC")
    suspend fun idsInBucket(bucketId: Long): List<Long>
}
