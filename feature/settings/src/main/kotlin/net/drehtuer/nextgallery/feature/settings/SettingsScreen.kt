package net.drehtuer.nextgallery.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

/** The Settings root. [accountLabel] is the Nextcloud account, null if there is none. */
@Composable
fun SettingsScreen(
    accountLabel: String?,
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenNextcloud: () -> Unit,
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
                title = stringResource(R.string.feature_settings_nextcloud),
                detail = accountLabel ?: stringResource(R.string.feature_settings_not_connected),
                icon = R.drawable.feature_settings_ic_cloud,
                onClick = onOpenNextcloud,
            )
            SettingsRow(
                title = stringResource(R.string.feature_settings_about),
                icon = R.drawable.feature_settings_ic_info,
                onClick = onOpenAbout,
            )
        }
    }
}
