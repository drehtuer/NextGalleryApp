package net.drehtuer.nextgallery.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MediaEntity::class, SyncStateEntity::class, RemoteCacheEntity::class, UploadStateEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class NextGalleryDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    abstract fun folderDao(): FolderDao

    abstract fun timelineDao(): TimelineDao

    abstract fun geoDao(): GeoDao

    abstract fun syncStateDao(): SyncStateDao

    abstract fun remoteCacheDao(): RemoteCacheDao

    abstract fun uploadStateDao(): UploadStateDao

    companion object {
        const val NAME = "nextgallery.db"
    }
}
