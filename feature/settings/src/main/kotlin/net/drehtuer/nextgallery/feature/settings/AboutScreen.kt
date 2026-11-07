package net.drehtuer.nextgallery.feature.settings

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.data.settings.LibraryLicence
import net.drehtuer.nextgallery.core.data.settings.LicenceCatalog
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

internal const val LICENCES_LIST_TAG = "licences-list"

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

/**
 * [libraries] comes from the list the build generates from the dependencies; it is null while that is loading. Map and
 * place data are no dependencies, so their entries are kept by hand.
 */
@Composable
fun LicencesScreen(libraries: List<LibraryLicence>?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dataSources = stringArrayResource(R.array.feature_settings_data_source_entries)
    val dimensions = NextGalleryTheme.dimensions
    SettingsScaffold(stringResource(R.string.feature_settings_licences), onBack, modifier) { contentModifier ->
        LazyColumn(
            modifier = contentModifier.testTag(LICENCES_LIST_TAG),
            contentPadding = PaddingValues(dimensions.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        ) {
            items(dataSources) { entry ->
                SettingsRow(title = entry.substringBefore('|'), detail = entry.substringAfter('|'))
            }
            if (libraries == null) {
                item { LoadingIndicator() }
            } else {
                items(libraries, key = { it.id }) { library ->
                    SettingsRow(
                        title = library.name,
                        detail = stringResource(
                            R.string.feature_settings_licence_detail,
                            library.version,
                            library.licences.joinToString(),
                        ),
                    )
                }
            }
        }
    }
}

/** The generated list is a file in the APK, so it is read and parsed away from the main thread. */
@Composable
internal fun rememberLibraries(@RawRes json: Int): List<LibraryLicence>? {
    val resources = LocalResources.current
    val libraries by produceState<List<LibraryLicence>?>(null, json, resources) {
        value = withContext(Dispatchers.IO) {
            LicenceCatalog.parse(resources.openRawResource(json).bufferedReader().use { it.readText() })
        }
    }
    return libraries
}
