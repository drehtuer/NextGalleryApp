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

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    abstract fun bindDispatcherProvider(provider: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    abstract fun bindClock(clock: SystemClock): Clock

    @Binds
    abstract fun bindWorkScheduler(scheduler: WorkManagerScheduler): WorkScheduler

    companion object {
        @Provides
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
    }
}
