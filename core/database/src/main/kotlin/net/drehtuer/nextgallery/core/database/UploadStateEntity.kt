package net.drehtuer.nextgallery.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * What the last upload-state check found for an image. A row goes away with its image, and all rows at logout (D35).
 * Any change needs a migration (see [MIGRATION_2_3]).
 */
@Entity(
    tableName = "upload_state",
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class UploadStateEntity(
    @PrimaryKey @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "remote_path") val remotePath: String,
    val state: Int,
    @ColumnInfo(name = "checksum_verified") val checksumVerified: Boolean,
    @ColumnInfo(name = "local_size") val localSize: Long,
    @ColumnInfo(name = "local_date_modified_s") val localDateModifiedSeconds: Long,
    @ColumnInfo(name = "remote_etag") val remoteEtag: String?,
    @ColumnInfo(name = "remote_folder_etag") val remoteFolderEtag: String?,
    @ColumnInfo(name = "checked_at_ms") val checkedAtMillis: Long,
) {
    companion object {
        const val STATE_UNKNOWN = 0
        const val STATE_NOT_UPLOADED = 1
        const val STATE_UPLOADED = 2
    }
}

/** The part of a row the badges need. */
data class UploadStatusRow(
    @ColumnInfo(name = "media_id") val mediaId: Long,
    val state: Int,
    @ColumnInfo(name = "checksum_verified") val checksumVerified: Boolean,
)
