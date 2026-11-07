package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopBarsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backButton_clicked_invokesCallback() {
        var clicks = 0
        composeRule.setContent { BackButton(onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun shareAction_clicked_invokesCallback() {
        var clicks = 0
        composeRule.setContent { ShareAction(onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Share").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun selectAction_clicked_invokesCallback() {
        var clicks = 0
        composeRule.setContent { SelectAction(onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Select").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun selectAllAction_clicked_invokesCallback() {
        var clicks = 0
        composeRule.setContent { SelectAllAction(onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Select all").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun settingsAction_clicked_invokesCallback() {
        var clicks = 0
        composeRule.setContent { SettingsAction(onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun deleteAction_clicked_invokesCallback() {
        var clicks = 0
        composeRule.setContent { DeleteAction(onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Delete").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun selectionOverviewAction_clicked_invokesCallback() {
        var clicks = 0
        composeRule.setContent { SelectionOverviewAction(onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Show selection").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun selectionTopBar_showsTotalCount() {
        composeRule.setContent { SelectionTopBar(selectionCount = 3, onClear = {}) }

        composeRule.onNodeWithText("3 selected").assertIsDisplayed()
    }

    @Test
    fun selectionTopBar_clearClicked_invokesCallback() {
        var clears = 0
        composeRule.setContent { SelectionTopBar(selectionCount = 1, onClear = { clears++ }) }

        composeRule.onNodeWithContentDescription("Clear selection").performClick()

        assertEquals(1, clears)
    }

    @Test
    fun selectionTopBar_showsActions() {
        composeRule.setContent { SelectionTopBar(selectionCount = 1, onClear = {}, actions = { Text("action") }) }

        composeRule.onNodeWithText("action").assertIsDisplayed()
    }
}
