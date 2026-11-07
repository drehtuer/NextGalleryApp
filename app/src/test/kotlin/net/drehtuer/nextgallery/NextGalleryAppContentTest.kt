package net.drehtuer.nextgallery

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.MediaAccess
import net.drehtuer.nextgallery.core.ui.component.ScreenBanner
import net.drehtuer.nextgallery.feature.editor.EditorRoute
import net.drehtuer.nextgallery.feature.folders.SelectionOverviewRoute
import net.drehtuer.nextgallery.feature.remote.RemoteViewerRoute
import net.drehtuer.nextgallery.feature.timeline.TimelineRoute
import net.drehtuer.nextgallery.feature.viewer.ViewerRoute
import net.drehtuer.nextgallery.navigation.FoldersRoute
import net.drehtuer.nextgallery.navigation.SettingsGraphRoute
import net.drehtuer.nextgallery.navigation.settingsGraph
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Plain Application: these tests need no Hilt components.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class NextGalleryAppContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(access: MediaAccess?) = composeRule.setContent {
        NextGalleryAppContent(
            state = MainUiState(access),
            onRequestAccess = {},
            onOpenSettings = {},
            folders = {
                composable<FoldersRoute> {
                    Column {
                        ScreenBanner()
                        Text("Folders screen")
                    }
                }
            },
            timeline = { composable<TimelineRoute> { Text("Timeline screen") } },
        )
    }

    @Test
    fun accessUnknown_showsLoadingIndicator() {
        show(access = null)

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun accessDenied_showsAccessRequest() {
        show(MediaAccess.DENIED)

        composeRule.onNodeWithText("Allow access").assertIsDisplayed()
    }

    @Test
    fun partialAccess_showsLimitedAccessBanner() {
        show(MediaAccess.PARTIAL)

        composeRule.onNodeWithText("Select more").assertIsDisplayed()
    }

    @Test
    fun fullAccess_startsOnFolders() {
        show(MediaAccess.FULL)

        composeRule.onNodeWithText("Folders screen").assertIsDisplayed()
    }

    @Test
    fun viewerOpen_hidesBottomNavigation() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NextGalleryAppContent(
                state = MainUiState(MediaAccess.FULL),
                onRequestAccess = {},
                onOpenSettings = {},
                navController = navController,
                folders = { composable<FoldersRoute> { Text("Folders screen") } },
                shared = { composable<ViewerRoute> { Text("Viewer screen") } },
            )
        }

        composeRule.runOnIdle { navController.navigate(ViewerRoute(mediaId = 1, bucketId = 7)) }

        composeRule.onNodeWithText("Timeline").assertDoesNotExist()
    }

    @Test
    fun viewerRouteWithoutBucket_keepsNullThroughNavigation() {
        val navController = showSharedStandIns()

        composeRule.runOnIdle { navController.navigate(ViewerRoute(mediaId = 1, bucketId = null)) }

        composeRule.onNodeWithText("Viewer of bucket null").assertIsDisplayed()
    }

    @Test
    fun viewerRouteWithBucket_keepsBucketThroughNavigation() {
        val navController = showSharedStandIns()

        composeRule.runOnIdle { navController.navigate(ViewerRoute(mediaId = 1, bucketId = 7)) }

        composeRule.onNodeWithText("Viewer of bucket 7").assertIsDisplayed()
    }

    @Test
    fun selectionOverviewRouteWithoutBucket_keepsNullThroughNavigation() {
        val navController = showSharedStandIns()

        composeRule.runOnIdle { navController.navigate(SelectionOverviewRoute(bucketId = null)) }

        composeRule.onNodeWithText("Overview of bucket null").assertIsDisplayed()
    }

    @Test
    fun selectionOverviewOpen_hidesBottomNavigation() {
        val navController = showSharedStandIns()

        composeRule.runOnIdle { navController.navigate(SelectionOverviewRoute(bucketId = null)) }

        composeRule.onNodeWithText("Timeline").assertDoesNotExist()
    }

    @Test
    fun editorOpen_hidesBottomNavigation() {
        val navController = showSharedStandIns()

        composeRule.runOnIdle { navController.navigate(EditorRoute(mediaId = 1)) }

        composeRule.onNodeWithText("Timeline").assertDoesNotExist()
    }

    @Test
    fun remoteViewerOpen_hidesBottomNavigation() {
        val navController = showSharedStandIns()

        composeRule.runOnIdle { navController.navigate(RemoteViewerRoute("/a.jpg")) }

        composeRule.onNodeWithText("Timeline").assertDoesNotExist()
    }

    private fun showSharedStandIns(): NavHostController {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NextGalleryAppContent(
                state = MainUiState(MediaAccess.FULL),
                onRequestAccess = {},
                onOpenSettings = {},
                navController = navController,
                folders = { composable<FoldersRoute> { Text("Folders screen") } },
                shared = {
                    composable<ViewerRoute> { Text("Viewer of bucket ${it.toRoute<ViewerRoute>().bucketId}") }
                    composable<SelectionOverviewRoute> {
                        Text("Overview of bucket ${it.toRoute<SelectionOverviewRoute>().bucketId}")
                    }
                    composable<EditorRoute> { Text("Editor screen") }
                },
                remote = { composable<RemoteViewerRoute> { Text("Remote viewer") } },
            )
        }
        return navController
    }

    @Test
    fun settingsOpen_hidesBottomNavigation() {
        val navController = showSettingsGraph()

        composeRule.runOnIdle { navController.navigate(SettingsGraphRoute) }

        composeRule.onNodeWithText("Timeline").assertDoesNotExist()
    }

    @Test
    fun settingsGraph_startsOnSettingsRoot() {
        val navController = showSettingsGraph()

        composeRule.runOnIdle { navController.navigate(SettingsGraphRoute) }

        composeRule.onNodeWithText("Include, exclude and hidden folders").assertIsDisplayed()
    }

    @Test
    fun settingsGraph_aboutShowsTheAppVersion() {
        val navController = showSettingsGraph()
        composeRule.runOnIdle { navController.navigate(SettingsGraphRoute) }

        composeRule.onNodeWithText("About & licences").performScrollTo().performClick()

        composeRule.onNodeWithText("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})").assertIsDisplayed()
    }

    @Test
    fun settingsGraph_licencesOpenFromAbout() {
        val navController = showSettingsGraph()
        composeRule.runOnIdle { navController.navigate(SettingsGraphRoute) }
        composeRule.onNodeWithText("About & licences").performScrollTo().performClick()

        composeRule.onNodeWithText("Open-source licences").performScrollTo().performClick()

        composeRule.onNodeWithText("AndroidX and Jetpack Compose").assertIsDisplayed()
    }

    /** The real settings graph. Its root, About and licences screens need no Hilt. */
    private fun showSettingsGraph(): NavHostController {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NextGalleryAppContent(
                state = MainUiState(MediaAccess.FULL),
                onRequestAccess = {},
                onOpenSettings = {},
                navController = navController,
                folders = { composable<FoldersRoute> { Text("Folders screen") } },
                settings = { settingsGraph(it, accountLabel = { null }) },
            )
        }
        return navController
    }

    @Test
    fun timelineTabClicked_showsTimeline() {
        show(MediaAccess.FULL)

        composeRule.onNodeWithText("Timeline").performClick()

        composeRule.onNodeWithText("Timeline screen").assertIsDisplayed()
    }
}
