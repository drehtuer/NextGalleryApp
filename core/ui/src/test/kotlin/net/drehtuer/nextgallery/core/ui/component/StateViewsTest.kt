package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.material3.Button
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
class StateViewsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingIndicator_hasLoadingDescription() {
        composeRule.setContent { LoadingIndicator() }

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsTitle() {
        composeRule.setContent { EmptyState("No photos") }

        composeRule.onNodeWithText("No photos").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsDetail() {
        composeRule.setContent { EmptyState("No photos", detail = "Take one") }

        composeRule.onNodeWithText("Take one").assertIsDisplayed()
    }

    @Test
    fun emptyState_action_isShownAndClickable() {
        var clicks = 0
        composeRule.setContent {
            EmptyState("No photos", action = { Button(onClick = { clicks++ }) { Text("Add folder") } })
        }

        composeRule.onNodeWithText("Add folder").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun errorState_showsMessage() {
        composeRule.setContent { ErrorState("Failed", "Restart the phone", onRetry = {}) }

        composeRule.onNodeWithText("Restart the phone").assertIsDisplayed()
    }

    @Test
    fun errorState_tryAgainClicked_invokesCallback() {
        var retries = 0
        composeRule.setContent { ErrorState("Failed", "Restart the phone", onRetry = { retries++ }) }

        composeRule.onNodeWithText("Try again").performClick()

        assertEquals(1, retries)
    }
}
