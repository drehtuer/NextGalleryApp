package net.drehtuer.nextgallery.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

/** The map's tile server (D23). */
@Composable
fun MapTilesScreen(
    state: MapTilesUiState,
    onEvent: (MapTilesEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(stringResource(R.string.feature_settings_map_tiles), onBack, modifier) { contentModifier ->
        SettingsColumn(contentModifier) {
            OutlinedTextField(
                value = state.url,
                onValueChange = { onEvent(MapTilesEvent.UrlChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feature_settings_map_tiles_label)) },
                isError = !state.valid,
                supportingText = if (state.valid) {
                    null
                } else {
                    { Text(stringResource(R.string.feature_settings_map_tiles_invalid)) }
                },
            )
            SettingsNote(stringResource(R.string.feature_settings_map_tiles_hint))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NextGalleryTheme.dimensions.spacingSmall, Alignment.End),
            ) {
                TextButton(onClick = { onEvent(MapTilesEvent.UseDefault) }) {
                    Text(stringResource(R.string.feature_settings_map_tiles_reset))
                }
                Button(onClick = { onEvent(MapTilesEvent.Save) }, enabled = state.edited && state.valid) {
                    Text(stringResource(R.string.feature_settings_map_tiles_save))
                }
            }
        }
    }
}
