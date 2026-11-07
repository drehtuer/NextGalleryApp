package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteFolderRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsNameAndDetail() {
        composeRule.setContent { RemoteFolderRow("Italy", onClick = {}, detail = "12 images") }

        composeRule.onNodeWithText("Italy").assertIsDisplayed()
        composeRule.onNodeWithText("12 images").assertIsDisplayed()
    }

    @Test
    fun clicked_callsOnClick() {
        var clicked = false
        composeRule.setContent { RemoteFolderRow("Italy", onClick = { clicked = true }) }

        composeRule.onNodeWithText("Italy").performClick()

        assertTrue(clicked)
    }

    @Test
    fun disabled_cannotBeClicked() {
        composeRule.setContent { RemoteFolderRow("Italy", onClick = {}, enabled = false) }

        composeRule.onNodeWithText("Italy").assertIsNotEnabled()
    }
}
