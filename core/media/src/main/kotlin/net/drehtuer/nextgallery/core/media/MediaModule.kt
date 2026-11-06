package net.drehtuer.nextgallery.core.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.access.MediaPermissionSource
import net.drehtuer.nextgallery.core.data.index.ExifSource
import net.drehtuer.nextgallery.core.data.index.MediaChangeSource
import net.drehtuer.nextgallery.core.data.index.MediaScanSource
import net.drehtuer.nextgallery.core.data.media.MediaDeleteSource

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
    fun bindMediaDeleteSource(deleter: MediaStoreDeleter): MediaDeleteSource

    @Binds
    fun bindMediaPermissionSource(source: AndroidMediaPermissionSource): MediaPermissionSource
}
