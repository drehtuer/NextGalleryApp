package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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

    @Test
    fun chips_haveATouchTargetLargerThanThePill() {
        show("/Photos/2024")

        composeRule.onAllNodes(hasClickAction()).assertAll(namedTouchTarget)
    }

    @Test
    fun row_keepsTheDesignedHeight() {
        composeRule.setContent { Breadcrumbs("/Photos", onOpen = {}, modifier = Modifier.testTag("row")) }

        composeRule.onNodeWithTag("row").assertHeightIsEqualTo(48.dp)
    }
}

/** A screen reader can say what it does, and a finger can hit it. */
private val namedTouchTarget = SemanticsMatcher("has a name and a 48 dp touch target") { node ->
    val named = SemanticsProperties.Text in node.config || SemanticsProperties.ContentDescription in node.config
    val minimum = with(node.layoutInfo.density) { 48.dp.roundToPx() }
    // The touch bounds are empty for what is scrolled out of the window, so these count the layout size as well.
    val touch = node.touchBoundsInRoot
    named && maxOf(touch.width.toInt(), node.size.width) >= minimum &&
        maxOf(touch.height.toInt(), node.size.height) >= minimum
}
