package net.drehtuer.nextgallery.core.platform

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.data.settings.DefaultFolderSettingsRepository
import net.drehtuer.nextgallery.core.data.settings.DefaultSettingsRepository
import net.drehtuer.nextgallery.core.data.settings.FolderSettingsRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository

/** Binds the repositories on top of `SettingsDataSource`. */
@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {
    @Binds
    fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    fun bindFolderSettingsRepository(repository: DefaultFolderSettingsRepository): FolderSettingsRepository
}
