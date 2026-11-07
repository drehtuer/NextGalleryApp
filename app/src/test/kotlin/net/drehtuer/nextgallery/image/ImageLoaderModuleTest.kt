package net.drehtuer.nextgallery.image

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.network.NetworkFetcher
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.ui.thumbnail.MediaThumbnailFetcher
import okhttp3.Call
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Plain Application: this test needs no Hilt components.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ImageLoaderModuleTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val loader = buildImageLoader(context)
    private val remoteLoader = buildRemoteImageLoader(context) { error("The client is built on the first request") }

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

    @Test
    fun buildImageLoader_hasNoNetworkFetcher() {
        assertFalse(loader.components.fetcherFactories.any { it.first is NetworkFetcher.Factory })
    }

    @Test
    fun buildRemoteImageLoader_limitsDiskCacheTo500Megabytes() {
        assertEquals(500L * 1024 * 1024, remoteLoader.diskCache?.maxSize)
    }

    @Test
    fun buildRemoteImageLoader_hasItsOwnDiskCacheDirectory() {
        assertEquals(REMOTE_DISK_CACHE_DIRECTORY, remoteLoader.diskCache?.directory?.name)
    }

    @Test
    fun buildRemoteImageLoader_registersTheNetworkFetcher() {
        assertEquals(1, remoteLoader.components.fetcherFactories.count { it.first is NetworkFetcher.Factory })
    }

    @Test
    fun coilRemoteImageCache_clear_emptiesDiskCache() = runTest {
        val disk = checkNotNull(remoteLoader.diskCache)
        disk.openEditor("key")?.commitAndOpenSnapshot()?.close()

        CoilRemoteImageCache(remoteLoader).clear()

        assertNull(disk.openSnapshot("key"))
    }
}
