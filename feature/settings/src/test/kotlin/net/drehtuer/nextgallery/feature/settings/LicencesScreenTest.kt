package net.drehtuer.nextgallery.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LicencesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun entries_showLibraryAndLicence() {
        composeRule.setContent { LicencesScreen(onBack = {}) }

        composeRule.onNodeWithText("Coil").assertIsDisplayed()
        composeRule.onAllNodesWithText("Apache-2.0").onFirst().assertIsDisplayed()
    }

    @Test
    fun backClicked_goesBack() {
        var clicks = 0
        composeRule.setContent { LicencesScreen(onBack = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, clicks)
    }
}
