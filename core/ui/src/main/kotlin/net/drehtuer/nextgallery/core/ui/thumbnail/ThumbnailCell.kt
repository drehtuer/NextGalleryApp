package net.drehtuer.nextgallery.core.ui.thumbnail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryOverlayColors
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

internal const val THUMBNAIL_PLACEHOLDER_TAG = "thumbnail-placeholder"
internal const val THUMBNAIL_FAILED_TAG = "thumbnail-failed"
internal const val SELECTION_RING_TAG = "selection-ring"
private const val GREYED_OUT_ALPHA = 0.38f

/**
 * A square grid cell. [thumbnail] is null while the item is not loaded yet (paging placeholder).
 * [selecting] shows an empty ring on unselected cells, so the user can tell that a selection is in progress.
 * [greyedOut] marks an image the current view cannot show (selection overview, D33).
 */
@Composable
fun ThumbnailCell(
    thumbnail: MediaThumbnail?,
    contentDescription: String?,
    sizePx: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selecting: Boolean = false,
    greyedOut: Boolean = false,
    badge: @Composable BoxScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val dimensions = NextGalleryTheme.dimensions
    val unavailable = stringResource(R.string.core_ui_thumbnail_unavailable)
    var failed by remember(thumbnail) { mutableStateOf(false) }
    Box(
        modifier
            .aspectRatio(1f)
            .background(if (failed) colors.errorContainer else colors.surfaceVariant)
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
                contentScale = ContentScale.Crop,
                alpha = if (greyedOut) GREYED_OUT_ALPHA else 1f,
                onError = { failed = true },
            )
        }
        if (failed) {
            Icon(
                painterResource(R.drawable.core_ui_ic_broken_image),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).testTag(THUMBNAIL_FAILED_TAG),
                tint = colors.onErrorContainer,
            )
        }
        badge()
        if (selected) Box(Modifier.fillMaxSize().border(dimensions.selectionBorderWidth, colors.primary))
        if (selected || selecting) SelectionMark(selected, Modifier.align(Alignment.TopEnd))
    }
}

/** Filled with a check when selected; otherwise an empty white ring on a dark backing, readable over any photo. */
@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    val colors = MaterialTheme.colorScheme
    val mark = Modifier
        .padding(dimensions.selectionRingInset)
        .size(dimensions.selectionRingSize)
    if (selected) {
        Box(modifier.then(mark).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.core_ui_ic_check),
                contentDescription = stringResource(R.string.core_ui_thumbnail_selected),
                modifier = Modifier.size(dimensions.selectionIconSize),
                tint = colors.onPrimary,
            )
        }
    } else {
        Box(
            modifier
                .then(mark)
                .testTag(SELECTION_RING_TAG)
                .background(NextGalleryOverlayColors.badgeBacking, CircleShape)
                .border(dimensions.selectionRingStroke, NextGalleryOverlayColors.badgeOn, CircleShape),
        )
    }
}
