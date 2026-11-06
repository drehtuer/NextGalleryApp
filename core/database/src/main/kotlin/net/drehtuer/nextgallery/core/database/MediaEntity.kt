package net.drehtuer.nextgallery.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import net.drehtuer.nextgallery.core.model.DateSource

/** Row of the `media` table: an indexed image (ARCHITECTURE.md §4.1). */
@Entity(
    tableName = "media",
    indices = [
        Index("bucket_id", "date_taken_ms"),
        Index("date_taken_ms"),
        Index("local_year", "local_month", "local_day"),
        Index("volume_name"),
    ],
)
data class MediaEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "volume_name") val volumeName: String,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    @ColumnInfo(name = "bucket_id") val bucketId: Long,
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val orientation: Int,
    @ColumnInfo(name = "date_taken_ms") val dateTakenMillis: Long,
    @ColumnInfo(name = "date_source") val dateSource: DateSource,
    @ColumnInfo(name = "date_modified_s") val dateModifiedSeconds: Long,
    @ColumnInfo(name = "generation_modified") val generationModified: Long,
    @ColumnInfo(name = "local_year") val localYear: Int,
    @ColumnInfo(name = "local_month") val localMonth: Int,
    @ColumnInfo(name = "local_day") val localDay: Int,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "exif_scanned") val exifScanned: Boolean,
)
