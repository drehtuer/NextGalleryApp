package net.drehtuer.nextgallery.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import net.drehtuer.nextgallery.BuildConfig
import net.drehtuer.nextgallery.R
import net.drehtuer.nextgallery.feature.folders.FolderGridRoute
import net.drehtuer.nextgallery.feature.folders.FolderListRoute
import net.drehtuer.nextgallery.feature.folders.SelectionOverviewRoute
import net.drehtuer.nextgallery.feature.folders.folderGridScreen
import net.drehtuer.nextgallery.feature.folders.folderListScreen
import net.drehtuer.nextgallery.feature.folders.selectionOverviewScreen
import net.drehtuer.nextgallery.feature.settings.AboutInfo
import net.drehtuer.nextgallery.feature.settings.AboutRoute
import net.drehtuer.nextgallery.feature.settings.AppearanceRoute
import net.drehtuer.nextgallery.feature.settings.FolderRulesRoute
import net.drehtuer.nextgallery.feature.settings.LicencesRoute
import net.drehtuer.nextgallery.feature.settings.SettingsRoute
import net.drehtuer.nextgallery.feature.settings.aboutScreen
import net.drehtuer.nextgallery.feature.settings.appearanceScreen
import net.drehtuer.nextgallery.feature.settings.folderRulesScreen
import net.drehtuer.nextgallery.feature.settings.licencesScreen
import net.drehtuer.nextgallery.feature.settings.settingsScreen
import net.drehtuer.nextgallery.feature.viewer.ViewerRoute
import net.drehtuer.nextgallery.feature.viewer.viewerScreen

/**
 * Navigation graph. The feature screens replace the placeholders in the following phases.
 * [folders] and [settings] build those destinations; tests replace them, because the real screens need Hilt.
 */
@Composable
fun NextGalleryNavHost(
    navController: NavHostController,
    folders: NavGraphBuilder.(NavHostController) -> Unit,
    settings: NavGraphBuilder.(NavHostController) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = FoldersRoute, modifier = modifier) {
        folders(navController)
        settings(navController)
        composable<TimelineRoute> { PlaceholderScreen(stringResource(R.string.destination_timeline)) }
        composable<MapRoute> { PlaceholderScreen(stringResource(R.string.destination_map)) }
        composable<NextcloudRoute> { PlaceholderScreen(stringResource(R.string.destination_nextcloud)) }
    }
}

/** A nested graph, so that the Folders tab stays selected and keeps its back stack while a folder is open. */
fun NavGraphBuilder.foldersGraph(navController: NavHostController) {
    navigation<FoldersRoute>(startDestination = FolderListRoute) {
        folderListScreen(
            onFolderClick = { bucketId -> navController.navigate(FolderGridRoute(bucketId)) },
            onOpenSettings = { navController.navigate(SettingsGraphRoute) },
        )
        folderGridScreen(
            onBack = navController::popBackStack,
            onOpenViewer = { id, bucketId -> navController.navigate(ViewerRoute(id.value, bucketId)) },
            onOpenSelectionOverview = { bucketId -> navController.navigate(SelectionOverviewRoute(bucketId)) },
        )
        selectionOverviewScreen(onBack = navController::popBackStack)
        viewerScreen(onBack = navController::popBackStack)
    }
}

fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    navigation<SettingsGraphRoute>(startDestination = SettingsRoute) {
        settingsScreen(
            onBack = navController::popBackStack,
            onOpenFolders = { navController.navigate(FolderRulesRoute) },
            onOpenAppearance = { navController.navigate(AppearanceRoute) },
            onOpenAbout = { navController.navigate(AboutRoute) },
        )
        folderRulesScreen(onBack = navController::popBackStack)
        appearanceScreen(onBack = navController::popBackStack)
        aboutScreen(
            info = AboutInfo(
                versionName = BuildConfig.VERSION_NAME,
                buildNumber = BuildConfig.VERSION_CODE,
                iconBackground = R.drawable.ic_launcher_background,
                iconForeground = R.drawable.ic_launcher_foreground,
            ),
            onBack = navController::popBackStack,
            onOpenLicences = { navController.navigate(LicencesRoute) },
        )
        licencesScreen(onBack = navController::popBackStack)
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.placeholder_coming_soon), style = MaterialTheme.typography.bodyLarge)
    }
}
