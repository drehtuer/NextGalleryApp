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
import net.drehtuer.nextgallery.R
import net.drehtuer.nextgallery.feature.folders.FolderGridRoute
import net.drehtuer.nextgallery.feature.folders.FolderListRoute
import net.drehtuer.nextgallery.feature.folders.folderGridScreen
import net.drehtuer.nextgallery.feature.folders.folderListScreen

/**
 * Navigation graph. The feature screens replace the placeholders in the following phases.
 * [folders] builds the Folders destination; tests replace it, because the real screens need Hilt.
 */
@Composable
fun NextGalleryNavHost(
    navController: NavHostController,
    folders: NavGraphBuilder.(NavHostController) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = FoldersRoute, modifier = modifier) {
        folders(navController)
        composable<TimelineRoute> { PlaceholderScreen(stringResource(R.string.destination_timeline)) }
        composable<MapRoute> { PlaceholderScreen(stringResource(R.string.destination_map)) }
        composable<NextcloudRoute> { PlaceholderScreen(stringResource(R.string.destination_nextcloud)) }
    }
}

/** A nested graph, so that the Folders tab stays selected and keeps its back stack while a folder is open. */
fun NavGraphBuilder.foldersGraph(navController: NavHostController) {
    navigation<FoldersRoute>(startDestination = FolderListRoute) {
        folderListScreen(onFolderClick = { bucketId -> navController.navigate(FolderGridRoute(bucketId)) })
        folderGridScreen(
            onBack = navController::popBackStack,
            onOpenViewer = { /* The viewer comes in Phase 3. */ },
        )
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
