package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.index.SyncStateStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    @Binds
    abstract fun bindMediaIndexStore(store: RoomMediaIndexStore): MediaIndexStore

    @Binds
    abstract fun bindSyncStateStore(store: RoomSyncStateStore): SyncStateStore

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): NextGalleryDatabase =
            Room.databaseBuilder(context, NextGalleryDatabase::class.java, NextGalleryDatabase.NAME).build()

        @Provides
        fun provideMediaDao(database: NextGalleryDatabase): MediaDao = database.mediaDao()

        @Provides
        fun provideSyncStateDao(database: NextGalleryDatabase): SyncStateDao = database.syncStateDao()
    }
}
