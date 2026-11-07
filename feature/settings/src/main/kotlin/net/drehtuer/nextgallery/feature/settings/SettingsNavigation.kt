package net.drehtuer.nextgallery.feature.settings

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data object FolderRulesRoute

@Serializable
data object AppearanceRoute

@Serializable
data object AboutRoute

@Serializable
data object LicencesRoute

fun NavGraphBuilder.settingsScreen(
    onBack: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    composable<SettingsRoute> { SettingsScreen(onBack, onOpenFolders, onOpenAppearance, onOpenAbout) }
}

fun NavGraphBuilder.folderRulesScreen(onBack: () -> Unit) {
    composable<FolderRulesRoute> {
        val viewModel = hiltViewModel<FolderRulesViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        FolderRulesScreen(state, viewModel::onEvent, onBack)
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

fun NavGraphBuilder.licencesScreen(onBack: () -> Unit) {
    composable<LicencesRoute> { LicencesScreen(onBack) }
}
