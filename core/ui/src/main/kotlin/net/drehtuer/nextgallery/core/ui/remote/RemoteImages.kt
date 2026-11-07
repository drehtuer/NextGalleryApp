package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.SingletonImageLoader
import net.drehtuer.nextgallery.core.model.RemoteItem

/** The image loader with the account's credentials. `:app` provides it; server images must always use this one. */
val LocalRemoteImageLoader: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }

@Composable
fun remoteImageLoader(): ImageLoader = LocalRemoteImageLoader.current ?: SingletonImageLoader.get(LocalContext.current)

/**
 * The key of a server image in the memory and disk caches. It contains the etag, so a changed file is loaded again
 * (ARCHITECTURE.md §9.2). Null [sizePx] is the original file.
 */
fun remoteImageCacheKey(item: RemoteItem, sizePx: Int?): String =
    "remote:${item.fileId ?: item.path}:${item.etag.orEmpty()}:${sizePx ?: "original"}"
