package net.drehtuer.nextgallery.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

@Composable
fun AboutScreen(info: AboutInfo, onBack: () -> Unit, onOpenLicences: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val sourceUrl = stringResource(R.string.feature_settings_source_code_url)
    SettingsScaffold(stringResource(R.string.feature_settings_about), onBack, modifier) { contentModifier ->
        SettingsColumn(contentModifier) {
            AppIcon(info)
            Text(stringResource(R.string.feature_settings_app_name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.feature_settings_about_tagline))
            SettingsRow(
                title = stringResource(R.string.feature_settings_version),
                detail = stringResource(R.string.feature_settings_version_value, info.versionName, info.buildNumber),
            )
            SettingsRow(
                title = stringResource(R.string.feature_settings_source_code),
                detail = sourceUrl,
                onClick = { uriHandler.openUri(sourceUrl) },
            )
            SettingsRow(title = stringResource(R.string.feature_settings_licences), onClick = onOpenLicences)
            SettingsNote(stringResource(R.string.feature_settings_attribution))
        }
    }
}

@Composable
private fun AppIcon(info: AboutInfo) {
    Box(Modifier.size(NextGalleryTheme.dimensions.iconTileSize).clip(MaterialTheme.shapes.large)) {
        Image(painterResource(info.iconBackground), contentDescription = null, Modifier.fillMaxSize())
        Image(painterResource(info.iconForeground), contentDescription = null, Modifier.fillMaxSize())
    }
}

/** A hand-kept list of the shipped libraries; task 10.5 replaces it with one generated from the dependencies. */
@Composable
fun LicencesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val entries = stringArrayResource(R.array.feature_settings_licence_entries)
    SettingsScaffold(stringResource(R.string.feature_settings_licences), onBack, modifier) { contentModifier ->
        SettingsColumn(contentModifier) {
            entries.forEach { entry ->
                SettingsRow(title = entry.substringBefore('|'), detail = entry.substringAfter('|'))
            }
        }
    }
}
