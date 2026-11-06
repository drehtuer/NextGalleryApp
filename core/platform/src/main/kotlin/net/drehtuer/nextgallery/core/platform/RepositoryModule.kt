package net.drehtuer.nextgallery.core.platform

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.access.DefaultMediaAccessRepository
import net.drehtuer.nextgallery.core.data.access.MediaAccessRepository
import net.drehtuer.nextgallery.core.data.index.DefaultIndexRepository
import net.drehtuer.nextgallery.core.data.index.IndexRepository
import net.drehtuer.nextgallery.core.data.media.DefaultMediaRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.rules.DefaultFolderRulesProvider
import net.drehtuer.nextgallery.core.data.rules.FolderRulesProvider

/** Binds the `:core:data` interfaces to their default implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindIndexRepository(repository: DefaultIndexRepository): IndexRepository

    @Binds
    abstract fun bindMediaRepository(repository: DefaultMediaRepository): MediaRepository

    @Binds
    abstract fun bindMediaAccessRepository(repository: DefaultMediaAccessRepository): MediaAccessRepository

    @Binds
    abstract fun bindFolderRulesProvider(provider: DefaultFolderRulesProvider): FolderRulesProvider
}
