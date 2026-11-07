package net.drehtuer.nextgallery.core.platform

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.upload.DefaultUploadHandoffRepository
import net.drehtuer.nextgallery.core.data.upload.DefaultUploadStateRepository
import net.drehtuer.nextgallery.core.data.upload.MediaContentSource
import net.drehtuer.nextgallery.core.data.upload.NextcloudAppSource
import net.drehtuer.nextgallery.core.data.upload.UploadHandoffRepository
import net.drehtuer.nextgallery.core.data.upload.UploadStateRepository
import net.drehtuer.nextgallery.core.data.upload.UploadStateStore
import net.drehtuer.nextgallery.core.database.RoomUploadStateStore
import javax.inject.Singleton

/** The upload status and the hand-off to the Nextcloud app (D31). */
@Module
@InstallIn(SingletonComponent::class)
interface UploadModule {
    @Binds
    fun bindUploadStateStore(store: RoomUploadStateStore): UploadStateStore

    @Binds
    fun bindMediaContentSource(source: ContentResolverContentSource): MediaContentSource

    @Binds
    fun bindNextcloudAppSource(source: PackageManagerNextcloudAppSource): NextcloudAppSource

    @Binds
    fun bindUploadStateRepository(repository: DefaultUploadStateRepository): UploadStateRepository

    /** One instance for all screens: it remembers a hand-off until the app returns from it. */
    @Binds
    @Singleton
    fun bindUploadHandoffRepository(repository: DefaultUploadHandoffRepository): UploadHandoffRepository
}
