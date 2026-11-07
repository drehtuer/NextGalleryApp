package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.RemoteItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteThumbnailCellTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val item = RemoteItem("/a.jpg", "a.jpg", false, fileId = 1)

    @Test
    fun noPreview_showsTheFailedState() {
        composeRule.setContent { RemoteThumbnailCell(item, url = null, sizePx = 256) }

        composeRule.onNodeWithTag(REMOTE_THUMBNAIL_FAILED_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun withPreview_hasTheFileNameAsDescription() {
        composeRule.setContent { RemoteThumbnailCell(item, url = "https://x/preview", sizePx = 256) }

        composeRule.onNodeWithContentDescription("a.jpg", substring = true).assertIsDisplayed()
    }

    @Test
    fun selected_isMarkedSelected() {
        composeRule.setContent { RemoteThumbnailCell(item, url = "https://x/preview", sizePx = 256, selected = true) }

        composeRule.onNodeWithContentDescription("a.jpg", substring = true).assertIsSelected()
    }

    @Test
    fun whileSelecting_showsTheRing() {
        composeRule.setContent { RemoteThumbnailCell(item, url = null, sizePx = 256, selecting = true) }

        composeRule.onNodeWithTag("selection-ring", useUnmergedTree = true).assertIsDisplayed()
    }
}
