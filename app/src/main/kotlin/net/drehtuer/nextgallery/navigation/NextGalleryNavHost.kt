package net.drehtuer.nextgallery.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.drehtuer.nextgallery.R

/** Navigation graph. The feature screens replace the placeholders in the following phases. */
@Composable
fun NextGalleryNavHost(navController: NavHostController, indexedCount: Int, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = FoldersRoute, modifier = modifier) {
        composable<FoldersRoute> {
            PlaceholderScreen(
                title = stringResource(R.string.destination_folders),
                details = pluralStringResource(R.plurals.indexed_images, indexedCount, indexedCount),
            )
        }
        composable<TimelineRoute> { PlaceholderScreen(stringResource(R.string.destination_timeline)) }
        composable<MapRoute> { PlaceholderScreen(stringResource(R.string.destination_map)) }
        composable<NextcloudRoute> { PlaceholderScreen(stringResource(R.string.destination_nextcloud)) }
    }
}

@Composable
private fun PlaceholderScreen(title: String, details: String = stringResource(R.string.placeholder_coming_soon)) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(details, style = MaterialTheme.typography.bodyLarge)
    }
}
