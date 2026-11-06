package net.drehtuer.nextgallery.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaEntity::class, SyncStateEntity::class], version = 1, exportSchema = true)
abstract class NextGalleryDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    abstract fun folderDao(): FolderDao

    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val NAME = "nextgallery.db"
    }
}
