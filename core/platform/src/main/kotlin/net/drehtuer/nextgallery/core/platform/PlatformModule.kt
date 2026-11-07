package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.common.DefaultDispatcherProvider
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.common.SystemClock
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import net.drehtuer.nextgallery.core.data.map.LocationProvider
import net.drehtuer.nextgallery.core.data.map.PlaceSource
import net.drehtuer.nextgallery.core.data.settings.SettingsDataSource
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    abstract fun bindDispatcherProvider(provider: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    abstract fun bindClock(clock: SystemClock): Clock

    @Binds
    abstract fun bindWorkScheduler(scheduler: WorkManagerScheduler): WorkScheduler

    @Binds
    abstract fun bindPlaceSource(source: AssetPlaceSource): PlaceSource

    @Binds
    abstract fun bindLocationProvider(provider: AndroidLocationProvider): LocationProvider

    companion object {
        @Provides
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

        /** One instance per file: DataStore does not allow two in a process. */
        @Provides
        @Singleton
        fun provideSettingsDataSource(@ApplicationContext context: Context): SettingsDataSource =
            SettingsDataSource.create(File(context.filesDir, "datastore/settings.preferences_pb"))
    }
}
