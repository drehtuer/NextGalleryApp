package net.drehtuer.nextgallery.feature.settings

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data object FoldersSettingsRoute

@Serializable
data object AppearanceRoute

@Serializable
data object MapTilesRoute

@Serializable
data object AboutRoute

@Serializable
data object LicencesRoute

/** The account shown in the Settings root. Tests replace it, because the real one needs Hilt. */
@Composable
fun accountLabelFromViewModel(): String? {
    val viewModel = hiltViewModel<SettingsRootViewModel>()
    val label by viewModel.accountLabel.collectAsStateWithLifecycle()
    return label
}

fun NavGraphBuilder.settingsScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenNextcloud: () -> Unit,
    onOpenMapTiles: () -> Unit,
    onOpenAbout: () -> Unit,
    accountLabel: @Composable () -> String? = { accountLabelFromViewModel() },
) {
    composable<SettingsRoute> {
        SettingsScreen(
            accountLabel(),
            onBack,
            onOpenFolders,
            onOpenAppearance,
            onOpenNextcloud,
            onOpenMapTiles,
            onOpenAbout,
        )
    }
}

fun NavGraphBuilder.mapTilesScreen(onBack: () -> Unit) {
    composable<MapTilesRoute> {
        val viewModel = hiltViewModel<MapTilesViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        MapTilesScreen(state, viewModel::onEvent, onBack)
    }
}

fun NavGraphBuilder.foldersSettingsScreen(onBack: () -> Unit) {
    composable<FoldersSettingsRoute> {
        val viewModel = hiltViewModel<FoldersSettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val pickFolder = rememberFolderPicker { viewModel.onEvent(FoldersSettingsEvent.FolderPicked(it)) }
        FoldersSettingsScreen(state, viewModel::onEvent, pickFolder, onBack)
    }
}

fun NavGraphBuilder.appearanceScreen(onBack: () -> Unit) {
    composable<AppearanceRoute> {
        val viewModel = hiltViewModel<AppearanceViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        AppearanceScreen(state, viewModel::onEvent, onBack)
    }
}

fun NavGraphBuilder.aboutScreen(info: AboutInfo, onBack: () -> Unit, onOpenLicences: () -> Unit) {
    composable<AboutRoute> { AboutScreen(info, onBack, onOpenLicences) }
}

/** [licencesJson] is the raw resource that the build generates in `:app`. */
fun NavGraphBuilder.licencesScreen(@RawRes licencesJson: Int, onBack: () -> Unit) {
    composable<LicencesRoute> { LicencesScreen(rememberLibraries(licencesJson), onBack) }
}
