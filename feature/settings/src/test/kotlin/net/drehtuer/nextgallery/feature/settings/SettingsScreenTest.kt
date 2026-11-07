package net.drehtuer.nextgallery.feature.settings

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var opened: String? = null

    @Before
    fun setUp() {
        composeRule.setContent {
            SettingsScreen(
                onBack = { opened = "back" },
                onOpenFolders = { opened = "folders" },
                onOpenAppearance = { opened = "appearance" },
                onOpenAbout = { opened = "about" },
            )
        }
    }

    @Test
    fun rows_showTheirSecondaryLines() {
        composeRule.onNodeWithText("Include, exclude and hidden folders").assertIsDisplayed()
        composeRule.onNodeWithText("Theme, grid columns and accent colour").assertIsDisplayed()
    }

    @Test
    fun nextcloudRow_showsNotConnectedAndCannotBeOpened() {
        composeRule.onNodeWithText("Not connected").assertIsDisplayed().assertHasNoClickAction()
    }

    @Test
    fun foldersRowClicked_opensFolders() {
        composeRule.onNodeWithText("Folders").performClick()

        assertEquals("folders", opened)
    }

    @Test
    fun appearanceRowClicked_opensAppearance() {
        composeRule.onNodeWithText("Appearance").performClick()

        assertEquals("appearance", opened)
    }

    @Test
    fun aboutRowClicked_opensAbout() {
        composeRule.onNodeWithText("About & licences").performClick()

        assertEquals("about", opened)
    }

    @Test
    fun backClicked_goesBack() {
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals("back", opened)
    }
}
