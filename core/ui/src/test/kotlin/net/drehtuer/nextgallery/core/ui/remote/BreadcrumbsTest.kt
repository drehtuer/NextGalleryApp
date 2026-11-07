package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BreadcrumbsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var opened: String? = null

    private fun show(path: String) = composeRule.setContent { Breadcrumbs(path, onOpen = { opened = it }) }

    @Test
    fun showsOneChipPerFolder() {
        show("/Photos/2024")

        composeRule.onNodeWithText("Nextcloud").assertIsDisplayed()
        composeRule.onNodeWithText("Photos").assertIsDisplayed()
        composeRule.onNodeWithText("2024").assertIsDisplayed()
    }

    @Test
    fun parentClicked_opensIt() {
        show("/Photos/2024")

        composeRule.onNodeWithText("Photos").performClick()

        assertEquals("/Photos", opened)
    }

    @Test
    fun rootClicked_opensTheRoot() {
        show("/Photos")

        composeRule.onNodeWithText("Nextcloud").performClick()

        assertEquals("/", opened)
    }

    @Test
    fun currentFolder_cannotBeClicked() {
        show("/Photos")

        composeRule.onNodeWithText("Photos").assertIsNotEnabled()
    }
}
