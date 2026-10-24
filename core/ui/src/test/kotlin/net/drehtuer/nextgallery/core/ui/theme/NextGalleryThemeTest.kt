package net.drehtuer.nextgallery.core.ui.theme

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NextGalleryThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nextGalleryTheme_rendersContent() {
        composeRule.setContent {
            NextGalleryTheme { Text("content") }
        }

        composeRule.onNodeWithText("content").assertIsDisplayed()
    }
}
