package net.drehtuer.nextgallery.image

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.drehtuer.nextgallery.core.ui.thumbnail.MediaThumbnailFetcher
import net.drehtuer.nextgallery.core.ui.thumbnail.MediaThumbnailKeyer
import javax.inject.Singleton

private const val MEMORY_CACHE_FRACTION = 0.2
private const val DISK_CACHE_BYTES = 250L * 1024 * 1024
internal const val DISK_CACHE_DIRECTORY = "image_cache"

/** The app's only Coil image loader (ARCHITECTURE.md §9.1). No crossfade: grids must stay fast while scrolling. */
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
    .crossfade(false)
    .build()

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader = buildImageLoader(context)
}
