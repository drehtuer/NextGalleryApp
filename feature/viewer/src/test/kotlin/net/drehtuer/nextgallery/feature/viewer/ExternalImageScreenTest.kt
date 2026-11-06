package net.drehtuer.nextgallery.feature.viewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalImageScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun externalImageScreen_showsImage() {
        composeRule.setContent { ExternalImageScreen("content://files/1") }

        composeRule.onNodeWithContentDescription("Opened image").assertIsDisplayed()
    }
}
