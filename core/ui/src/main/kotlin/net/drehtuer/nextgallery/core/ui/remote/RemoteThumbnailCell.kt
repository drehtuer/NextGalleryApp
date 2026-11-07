package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.thumbnail.SelectionMark

const val REMOTE_THUMBNAIL_FAILED_TAG = "remote-thumbnail-failed"
private const val OFFLINE_ALPHA = 0.6f

/**
 * A grid cell with a server preview. [url] is null if the server has none; [offline] dims it (DESIGN.md). It merges no
 * semantics of its own: the clickable parent does, and a merge here would keep the name and the action in two nodes.
 */
@Composable
fun RemoteThumbnailCell(
    item: RemoteItem,
    url: String?,
    sizePx: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selecting: Boolean = false,
    offline: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    var failed by remember(url) { mutableStateOf(url == null) }
    Box(
        modifier
            .aspectRatio(1f)
            .background(if (failed) colors.errorContainer else colors.surfaceVariant)
            .semantics { if (selected || selecting) this.selected = selected },
    ) {
        if (url != null) {
            val key = remoteImageCacheKey(item, sizePx)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .size(sizePx)
                    .memoryCacheKey(key)
                    .diskCacheKey(key)
                    .build(),
                imageLoader = remoteImageLoader(),
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                placeholder = ColorPainter(colors.surfaceVariant),
                contentScale = ContentScale.Crop,
                alpha = if (offline) OFFLINE_ALPHA else 1f,
                onError = { failed = true },
            )
        }
        if (failed) {
            Icon(
                painterResource(R.drawable.core_ui_ic_broken_image),
                // The image already has the description, if there is one.
                contentDescription = item.name.takeIf { url == null },
                modifier = Modifier.align(Alignment.Center).testTag(REMOTE_THUMBNAIL_FAILED_TAG),
                tint = colors.onErrorContainer,
            )
        }
        if (selected) {
            Box(Modifier.fillMaxSize().border(NextGalleryTheme.dimensions.selectionBorderWidth, colors.primary))
        }
        if (selected || selecting) SelectionMark(selected, Modifier.align(Alignment.TopEnd))
    }
}
