package net.drehtuer.nextgallery.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryViewerTheme

/** Shows one image another app asked to open. It need not be in the index, so it offers only zooming. */
@Composable
fun ExternalImageScreen(contentUri: String, modifier: Modifier = Modifier) {
    NextGalleryViewerTheme {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
            ZoomableAsyncImage(
                model = contentUri,
                contentDescription = stringResource(R.string.feature_viewer_external_image),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
