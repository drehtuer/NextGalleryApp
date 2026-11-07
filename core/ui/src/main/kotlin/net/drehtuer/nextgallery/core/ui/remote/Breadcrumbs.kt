package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

/** One chip per folder from the root to [path]; the current one is bold on the accent's container. */
@Composable
fun Breadcrumbs(path: String, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        RemotePaths.ancestors(path).forEach { ancestor ->
            val current = ancestor == path
            val colors = MaterialTheme.colorScheme
            val shape = MaterialTheme.shapes.small
            Box(
                Modifier
                    .heightIn(min = dimensions.breadcrumbHeight)
                    .background(if (current) colors.primaryContainer else colors.surfaceContainer, shape)
                    .clickable(enabled = !current) { onOpen(ancestor) }
                    .padding(horizontal = dimensions.spacingSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    RemotePaths.name(ancestor).ifEmpty { stringResource(R.string.core_ui_remote_root) },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    color = if (current) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
            }
        }
    }
}
