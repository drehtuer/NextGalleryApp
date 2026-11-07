package net.drehtuer.nextgallery.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

/** The Settings root. The Nextcloud row only shows that there is no account until Phase 8 adds the account screen. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenMapTiles: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(stringResource(CoreUiR.string.core_ui_settings), onBack, modifier) { contentModifier ->
        Column(contentModifier.verticalScroll(rememberScrollState())) {
            SettingsRow(
                title = stringResource(R.string.feature_settings_folders),
                detail = stringResource(R.string.feature_settings_folders_detail),
                icon = R.drawable.feature_settings_ic_folder,
                onClick = onOpenFolders,
            )
            SettingsRow(
                title = stringResource(R.string.feature_settings_appearance),
                detail = stringResource(R.string.feature_settings_appearance_detail),
                icon = R.drawable.feature_settings_ic_palette,
                onClick = onOpenAppearance,
            )
            SettingsRow(
                title = stringResource(R.string.feature_settings_map_tiles),
                detail = stringResource(R.string.feature_settings_map_tiles_detail),
                icon = R.drawable.feature_settings_ic_map,
                onClick = onOpenMapTiles,
            )
            SettingsRow(
                title = stringResource(R.string.feature_settings_nextcloud),
                detail = stringResource(R.string.feature_settings_not_connected),
                icon = R.drawable.feature_settings_ic_cloud,
            )
            SettingsRow(
                title = stringResource(R.string.feature_settings_about),
                icon = R.drawable.feature_settings_ic_info,
                onClick = onOpenAbout,
            )
        }
    }
}
