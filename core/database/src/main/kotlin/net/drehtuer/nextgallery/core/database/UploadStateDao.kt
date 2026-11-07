package net.drehtuer.nextgallery.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Rows whose image changed since the check are left out: what they say is about the previous version of the image. */
private const val CURRENT_STATUS_QUERY = """
    SELECT upload_state.media_id, upload_state.state, upload_state.checksum_verified
    FROM upload_state
    JOIN media ON media.id = upload_state.media_id
    WHERE upload_state.media_id IN (:ids)
        AND upload_state.local_size = media.size_bytes
        AND upload_state.local_date_modified_s = media.date_modified_s
"""

@Dao
interface UploadStateDao {
    @Upsert
    suspend fun upsertAll(rows: List<UploadStateEntity>)

    @Query("SELECT id FROM media WHERE id IN (:ids)")
    suspend fun existingMediaIds(ids: List<Long>): List<Long>

    /** An image deleted during the check must not fail the foreign key and with it the whole batch. */
    @Transaction
    suspend fun upsertExisting(rows: List<UploadStateEntity>) {
        val existing = existingMediaIds(rows.map { it.mediaId }).toSet()
        upsertAll(rows.filter { it.mediaId in existing })
    }

    @Query(CURRENT_STATUS_QUERY)
    fun observeStatusFor(ids: List<Long>): Flow<List<UploadStatusRow>>

    @Query(CURRENT_STATUS_QUERY)
    suspend fun statusFor(ids: List<Long>): List<UploadStatusRow>

    @Query("SELECT DISTINCT relative_path FROM media")
    suspend fun localFolders(): List<String>

    @Query("SELECT * FROM media WHERE relative_path = :relativePath")
    suspend fun mediaInFolder(relativePath: String): List<MediaEntity>

    @Query(
        """
        SELECT upload_state.* FROM upload_state
        JOIN media ON media.id = upload_state.media_id
        WHERE media.relative_path = :relativePath
        """,
    )
    suspend fun rowsInFolder(relativePath: String): List<UploadStateEntity>

    @Query("DELETE FROM upload_state")
    suspend fun clear()
}
