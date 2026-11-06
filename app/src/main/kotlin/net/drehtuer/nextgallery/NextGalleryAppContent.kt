package net.drehtuer.nextgallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.drehtuer.nextgallery.core.model.MediaAccess
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.permission.LimitedAccessBanner
import net.drehtuer.nextgallery.core.ui.permission.MediaAccessRequest
import net.drehtuer.nextgallery.feature.viewer.ViewerRoute
import net.drehtuer.nextgallery.navigation.NextGalleryNavHost
import net.drehtuer.nextgallery.navigation.TopLevelDestination
import net.drehtuer.nextgallery.navigation.foldersGraph

@Composable
fun NextGalleryAppContent(
    state: MainUiState,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    navController: NavHostController = rememberNavController(),
    folders: NavGraphBuilder.(NavHostController) -> Unit = { foldersGraph(it) },
) {
    when (state.access) {
        null -> LoadingIndicator()

        MediaAccess.DENIED -> MediaAccessRequest(onRequestAccess = onRequestAccess, onOpenSettings = onOpenSettings)

        MediaAccess.FULL, MediaAccess.PARTIAL -> {
            val currentEntry by navController.currentBackStackEntryAsState()
            val fullScreen = currentEntry?.destination?.hasRoute(ViewerRoute::class) == true
            Scaffold(bottomBar = { if (!fullScreen) BottomBar(navController) }) { padding ->
                Column(if (fullScreen) Modifier else Modifier.padding(padding).consumeWindowInsets(padding)) {
                    if (state.access == MediaAccess.PARTIAL && !fullScreen) {
                        LimitedAccessBanner(onSelectMore = onRequestAccess)
                    }
                    NextGalleryNavHost(navController, folders)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val currentEntry by navController.currentBackStackEntryAsState()
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentEntry?.destination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}
