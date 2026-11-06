package net.drehtuer.nextgallery

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.MediaAccess
import net.drehtuer.nextgallery.navigation.FoldersRoute
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
            folders = { composable<FoldersRoute> { Text("Folders screen") } },
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
    fun timelineTabClicked_showsTimeline() {
        show(MediaAccess.FULL)

        composeRule.onNodeWithText("Timeline").performClick()

        composeRule.onNodeWithText("Coming soon").assertIsDisplayed()
    }
}
