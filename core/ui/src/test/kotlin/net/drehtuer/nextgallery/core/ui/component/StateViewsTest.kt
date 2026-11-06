package net.drehtuer.nextgallery.core.ui.component

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
    fun emptyState_showsMessage() {
        composeRule.setContent { EmptyState("No photos") }

        composeRule.onNodeWithText("No photos").assertIsDisplayed()
    }

    @Test
    fun errorState_retryClicked_invokesCallback() {
        var retries = 0
        composeRule.setContent { ErrorState("Failed", onRetry = { retries++ }) }

        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }
}
