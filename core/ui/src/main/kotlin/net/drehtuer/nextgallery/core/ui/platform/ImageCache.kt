package net.drehtuer.nextgallery.core.ui.platform

import android.content.Context
import coil3.SingletonImageLoader

/** An overwritten image keeps its URI, so the viewer would still show the old picture from Coil's memory cache. */
fun clearImageMemoryCache(context: Context) {
    SingletonImageLoader.get(context).memoryCache?.clear()
}
