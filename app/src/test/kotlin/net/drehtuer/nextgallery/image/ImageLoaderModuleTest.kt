package net.drehtuer.nextgallery.image

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.ui.thumbnail.MediaThumbnailFetcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Plain Application: this test needs no Hilt components.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ImageLoaderModuleTest {
    private val loader = buildImageLoader(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun buildImageLoader_limitsDiskCacheTo250Megabytes() {
        assertEquals(250L * 1024 * 1024, loader.diskCache?.maxSize)
    }

    @Test
    fun buildImageLoader_storesDiskCacheInImageCacheDirectory() {
        assertEquals(DISK_CACHE_DIRECTORY, loader.diskCache?.directory?.name)
    }

    @Test
    fun buildImageLoader_hasMemoryCache() {
        assertTrue(checkNotNull(loader.memoryCache).maxSize > 0)
    }

    @Test
    fun buildImageLoader_registersThumbnailFetcher() {
        assertTrue(loader.components.fetcherFactories.any { it.first is MediaThumbnailFetcher.Factory })
    }
}
