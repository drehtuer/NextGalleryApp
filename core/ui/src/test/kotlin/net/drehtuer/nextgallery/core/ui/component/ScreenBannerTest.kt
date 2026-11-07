package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screenBanner_showsProvidedBanner() {
        composeRule.setContent {
            CompositionLocalProvider(LocalScreenBanner provides { Text("banner") }) { ScreenBanner() }
        }

        composeRule.onNodeWithText("banner").assertIsDisplayed()
    }
}
