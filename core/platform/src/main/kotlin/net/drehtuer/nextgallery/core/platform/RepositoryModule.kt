package net.drehtuer.nextgallery.core.platform

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.access.DefaultMediaAccessRepository
import net.drehtuer.nextgallery.core.data.access.MediaAccessRepository
import net.drehtuer.nextgallery.core.data.index.DefaultIndexRepository
import net.drehtuer.nextgallery.core.data.index.IndexRepository
import net.drehtuer.nextgallery.core.data.media.DefaultFolderRepository
import net.drehtuer.nextgallery.core.data.media.DefaultMediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.DefaultMediaRepository
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.selection.DefaultSelectionRepository
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.data.settings.DefaultSettingsRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.data.timeline.DefaultTimelineRepository
import net.drehtuer.nextgallery.core.data.timeline.TimelineRepository
import javax.inject.Singleton

/** Binds the `:core:data` interfaces to their default implementations. */
@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindIndexRepository(repository: DefaultIndexRepository): IndexRepository

    @Binds
    fun bindMediaRepository(repository: DefaultMediaRepository): MediaRepository

    @Binds
    fun bindTimelineRepository(repository: DefaultTimelineRepository): TimelineRepository

    @Binds
    fun bindMediaActionsRepository(repository: DefaultMediaActionsRepository): MediaActionsRepository

    @Binds
    fun bindFolderRepository(repository: DefaultFolderRepository): FolderRepository

    /** App-wide selection (D33): one instance for all screens. */
    @Binds
    @Singleton
    fun bindSelectionRepository(repository: DefaultSelectionRepository): SelectionRepository

    @Binds
    fun bindMediaAccessRepository(repository: DefaultMediaAccessRepository): MediaAccessRepository

    @Binds
    fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository
}
