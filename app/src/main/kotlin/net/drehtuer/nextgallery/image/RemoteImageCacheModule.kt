package net.drehtuer.nextgallery.image

import coil3.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.BuildConfig
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteImageCache
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpConfig

/** Logging out removes the previews and images of the account (D35). */
internal class CoilRemoteImageCache(private val loader: ImageLoader) : RemoteImageCache {
    override suspend fun clear() {
        loader.memoryCache?.clear()
        withContext(Dispatchers.IO) { loader.diskCache?.clear() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RemoteImageCacheModule {
    @Provides
    fun provideRemoteImageCache(@RemoteImageLoader loader: ImageLoader): RemoteImageCache = CoilRemoteImageCache(loader)

    /** The build type decides about cleartext (D19), so this comes from `:app`. */
    @Provides
    fun provideHttpConfig() = NextcloudHttpConfig(
        userAgent = "NextGalleryApp/${BuildConfig.VERSION_NAME}",
        allowCleartext = BuildConfig.DEBUG,
    )
}
