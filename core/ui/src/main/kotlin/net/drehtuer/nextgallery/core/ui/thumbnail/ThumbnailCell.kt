package net.drehtuer.nextgallery.core.ui.thumbnail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

internal const val THUMBNAIL_PLACEHOLDER_TAG = "thumbnail-placeholder"
private const val GREYED_OUT_ALPHA = 0.38f

/**
 * A square grid cell. [thumbnail] is null while the item is not loaded yet (paging placeholder).
 * [greyedOut] marks an image the current view cannot show (selection overview, D33).
 */
@Composable
fun ThumbnailCell(
    thumbnail: MediaThumbnail?,
    contentDescription: String?,
    sizePx: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    greyedOut: Boolean = false,
    badge: @Composable BoxScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val dimensions = NextGalleryTheme.dimensions
    val unavailable = stringResource(R.string.core_ui_thumbnail_unavailable)
    Box(
        modifier
            .aspectRatio(1f)
            .background(colors.surfaceVariant)
            .semantics(mergeDescendants = true) {
                if (selected) this.selected = true
                if (greyedOut) stateDescription = unavailable
            },
    ) {
        if (thumbnail == null) {
            Box(Modifier.fillMaxSize().testTag(THUMBNAIL_PLACEHOLDER_TAG))
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(thumbnail).size(sizePx).build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                placeholder = ColorPainter(colors.surfaceVariant),
                error = ColorPainter(colors.errorContainer),
                contentScale = ContentScale.Crop,
                alpha = if (greyedOut) GREYED_OUT_ALPHA else 1f,
            )
        }
        badge()
        if (selected) {
            Box(Modifier.fillMaxSize().border(dimensions.selectionBorderWidth, colors.primary))
            Icon(
                painter = painterResource(R.drawable.core_ui_ic_check_circle),
                contentDescription = stringResource(R.string.core_ui_thumbnail_selected),
                modifier = Modifier.align(Alignment.TopEnd).padding(dimensions.spacingSmall),
                tint = colors.primary,
            )
        }
    }
}
