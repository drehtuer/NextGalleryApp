package net.drehtuer.nextgallery.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A folder derived from `media`: its newest image and the number of images. */
data class FolderRow(@Embedded val cover: MediaEntity, @ColumnInfo(name = "item_count") val itemCount: Int)

@Dao
interface FolderDao {
    @Query(
        """
        SELECT media.*, folder.item_count AS item_count
        FROM (SELECT bucket_id AS folder_bucket_id, COUNT(*) AS item_count FROM media GROUP BY bucket_id) AS folder
        JOIN media ON media.id = (
            SELECT newest.id FROM media AS newest
            WHERE newest.bucket_id = folder.folder_bucket_id
            ORDER BY newest.date_taken_ms DESC, newest.id DESC
            LIMIT 1
        )
        ORDER BY media.bucket_name COLLATE NOCASE, media.bucket_id
        """,
    )
    fun observeFolders(): Flow<List<FolderRow>>
}
