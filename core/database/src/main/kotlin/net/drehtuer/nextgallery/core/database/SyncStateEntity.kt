package net.drehtuer.nextgallery.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey @ColumnInfo(name = "volume_name") val volumeName: String,
    @ColumnInfo(name = "last_generation") val lastGeneration: Long,
    @ColumnInfo(name = "media_store_version") val mediaStoreVersion: String,
    @ColumnInfo(name = "last_full_scan_ms") val lastFullScanMillis: Long,
)
