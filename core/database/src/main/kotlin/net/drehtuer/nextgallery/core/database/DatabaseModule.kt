package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.index.GeoIndexStore
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.index.SyncStateStore
import net.drehtuer.nextgallery.core.data.index.TimelineStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    @Binds
    abstract fun bindMediaIndexStore(store: RoomMediaIndexStore): MediaIndexStore

    @Binds
    abstract fun bindTimelineStore(store: RoomTimelineStore): TimelineStore

    @Binds
    abstract fun bindSyncStateStore(store: RoomSyncStateStore): SyncStateStore

    @Binds
    abstract fun bindGeoIndexStore(store: RoomGeoIndexStore): GeoIndexStore

    companion object {
        @Provides
        fun provideGeoDao(database: NextGalleryDatabase): GeoDao = database.geoDao()

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): NextGalleryDatabase =
            Room.databaseBuilder(context, NextGalleryDatabase::class.java, NextGalleryDatabase.NAME)
                .addMigrations(MIGRATION_1_2)
                .build()

        @Provides
        fun provideMediaDao(database: NextGalleryDatabase): MediaDao = database.mediaDao()

        @Provides
        fun provideFolderDao(database: NextGalleryDatabase): FolderDao = database.folderDao()

        @Provides
        fun provideTimelineDao(database: NextGalleryDatabase): TimelineDao = database.timelineDao()

        @Provides
        fun provideSyncStateDao(database: NextGalleryDatabase): SyncStateDao = database.syncStateDao()

        @Provides
        fun provideRemoteCacheDao(database: NextGalleryDatabase): RemoteCacheDao = database.remoteCacheDao()
    }
}
