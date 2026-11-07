package net.drehtuer.nextgallery.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** The last listing of a server folder, for offline browsing. Any change needs a migration (see [MIGRATION_1_2]). */
@Entity(tableName = "remote_cache", indices = [Index("parent_path")])
data class RemoteCacheEntity(
    @PrimaryKey val path: String,
    @ColumnInfo(name = "parent_path") val parentPath: String,
    val name: String,
    @ColumnInfo(name = "is_directory") val isDirectory: Boolean,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "content_type") val contentType: String?,
    val etag: String?,
    @ColumnInfo(name = "file_id") val fileId: Long?,
    val checksums: String?,
    val permissions: String?,
    @ColumnInfo(name = "has_preview") val hasPreview: Boolean,
    @ColumnInfo(name = "child_count") val childCount: Int?,
    @ColumnInfo(name = "last_modified_ms") val lastModifiedMillis: Long,
    @ColumnInfo(name = "fetched_at_ms") val fetchedAtMillis: Long,
)
