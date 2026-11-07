package net.drehtuer.nextgallery.image

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.serviceLoaderEnabled
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpModule
import net.drehtuer.nextgallery.core.ui.thumbnail.MediaThumbnailFetcher
import net.drehtuer.nextgallery.core.ui.thumbnail.MediaThumbnailKeyer
import okhttp3.Call
import javax.inject.Qualifier
import javax.inject.Singleton

private const val MEMORY_CACHE_FRACTION = 0.2
private const val DISK_CACHE_BYTES = 250L * 1024 * 1024
internal const val DISK_CACHE_DIRECTORY = "image_cache"

private const val REMOTE_MEMORY_CACHE_FRACTION = 0.1
private const val REMOTE_DISK_CACHE_BYTES = 500L * 1024 * 1024
internal const val REMOTE_DISK_CACHE_DIRECTORY = "remote_image_cache"

/**
 * The Coil image loader for the local images (ARCHITECTURE.md §9.1). No crossfade: grids must stay fast while
 * scrolling. No service loader, because that would add a network fetcher without the app's certificate rules.
 */
internal fun buildImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
    .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_FRACTION).build() }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve(DISK_CACHE_DIRECTORY))
            .maxSizeBytes(DISK_CACHE_BYTES)
            .build()
    }
    .components {
        add(MediaThumbnailKeyer())
        add(MediaThumbnailFetcher.Factory(context.contentResolver))
    }
    .serviceLoaderEnabled(false)
    .crossfade(false)
    .build()

/**
 * The loader for server images (D37): its own caches, so that logging out can wipe them without touching local
 * thumbnails, and all requests through the authenticated client. [callFactory] is only called on the first request.
 */
internal fun buildRemoteImageLoader(context: Context, callFactory: () -> Call.Factory): ImageLoader =
    ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, REMOTE_MEMORY_CACHE_FRACTION).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(REMOTE_DISK_CACHE_DIRECTORY))
                .maxSizeBytes(REMOTE_DISK_CACHE_BYTES)
                .build()
        }
        .components { add(OkHttpNetworkFetcherFactory(callFactory = callFactory)) }
        .serviceLoaderEnabled(false)
        .crossfade(false)
        .build()

/** The Coil image loader for server images, with the account's credentials. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteImageLoader

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader = buildImageLoader(context)

    @Provides
    @Singleton
    @RemoteImageLoader
    fun provideRemoteImageLoader(@ApplicationContext context: Context, http: Lazy<NextcloudHttpModule>): ImageLoader =
        buildRemoteImageLoader(context) { http.get().authenticated }
}
