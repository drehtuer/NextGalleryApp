package net.drehtuer.nextgallery.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

/** Shows one image another app asked to open. It need not be in the index, so it offers only zooming. */
@Composable
fun ExternalImageScreen(contentUri: String, modifier: Modifier = Modifier) {
    NextGalleryTheme(darkTheme = true) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
            ZoomableAsyncImage(model = contentUri, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
