package net.drehtuer.nextgallery.core.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.access.MediaPermissionSource
import net.drehtuer.nextgallery.core.data.index.ExifSource
import net.drehtuer.nextgallery.core.data.index.MediaChangeSource
import net.drehtuer.nextgallery.core.data.index.MediaScanSource

@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {
    @Binds
    fun bindMediaScanSource(scanner: MediaStoreScanner): MediaScanSource

    @Binds
    fun bindExifSource(source: AndroidExifSource): ExifSource

    @Binds
    fun bindMediaChangeSource(observer: MediaStoreChangeObserver): MediaChangeSource

    @Binds
    fun bindMediaPermissionSource(source: AndroidMediaPermissionSource): MediaPermissionSource
}
