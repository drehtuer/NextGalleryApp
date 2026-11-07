package net.drehtuer.nextgallery.core.ui.platform

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.memory.MemoryCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(DelicateCoilApi::class)
@RunWith(AndroidJUnit4::class)
class ImageCacheTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() = SingletonImageLoader.reset()

    @Test
    fun clearImageMemoryCache_removesCachedImages() {
        val loader = ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizeBytes(1_000_000L).build() }
            .build()
        SingletonImageLoader.setUnsafe(loader)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        loader.memoryCache!![MemoryCache.Key("uri")] = MemoryCache.Value(bitmap.asImage())

        clearImageMemoryCache(context)

        assertEquals(0, loader.memoryCache!!.keys.size)
    }
}
