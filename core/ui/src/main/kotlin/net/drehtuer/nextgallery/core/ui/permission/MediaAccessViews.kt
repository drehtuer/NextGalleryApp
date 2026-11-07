package net.drehtuer.nextgallery.core.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

/** Shown instead of the app content while the app has no access to photos. */
@Composable
fun MediaAccessRequest(onRequestAccess: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(dimensions.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(dimensions.iconTileSize)
                        .background(colors.primaryContainer, MaterialTheme.shapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.core_ui_ic_photo),
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconTileIconSize),
                        tint = colors.onPrimaryContainer,
                    )
                }
                Text(
                    stringResource(R.string.core_ui_media_access_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.core_ui_media_access_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.core_ui_media_access_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Button(onClick = onRequestAccess, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.core_ui_media_access_allow))
            }
            // When the user denied twice, the system no longer shows the dialog; only the settings page helps then.
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.core_ui_media_access_open_settings)) }
        }
    }
}

/** Card for partial access (only selected photos), with a way to select more (D15). */
@Composable
fun LimitedAccessBanner(onSelectMore: () -> Unit, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingSmall, vertical = dimensions.spacingSmall),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.core_ui_limited_access_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSelectMore) { Text(stringResource(R.string.core_ui_limited_access_select_more)) }
        }
    }
}
