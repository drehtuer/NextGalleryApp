package net.drehtuer.nextgallery.core.ui.thumbnail

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThumbnailCellTest {
    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        // This test has no thumbnail fetcher, so Coil reports an error shortly after the request starts.
        const val WAIT_MILLIS = 5_000L
    }

    private val thumbnail = MediaThumbnail(id = 1, contentUri = "content://media/1", dateModifiedSeconds = 100)

    @Test
    fun thumbnailCell_notLoaded_showsPlaceholder() {
        composeRule.setContent { ThumbnailCell(thumbnail = null, contentDescription = null, sizePx = 256) }

        composeRule.onNodeWithTag(THUMBNAIL_PLACEHOLDER_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun thumbnailCell_loaded_hasContentDescription() {
        composeRule.setContent { ThumbnailCell(thumbnail, contentDescription = "IMG_1.jpg", sizePx = 256) }

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).assertIsDisplayed()
    }

    @Test
    fun thumbnailCell_selected_isMarkedSelected() {
        composeRule.setContent { ThumbnailCell(thumbnail, "IMG_1.jpg", sizePx = 256, selected = true) }

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).assertIsSelected()
    }

    @Test
    fun thumbnailCell_selectingButNotSelected_showsEmptyRing() {
        composeRule.setContent { ThumbnailCell(thumbnail, "IMG_1.jpg", sizePx = 256, selecting = true) }

        composeRule.onNodeWithTag(SELECTION_RING_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun thumbnailCell_selected_showsNoEmptyRing() {
        composeRule.setContent {
            ThumbnailCell(thumbnail, "IMG_1.jpg", sizePx = 256, selected = true, selecting = true)
        }

        composeRule.onNodeWithTag(SELECTION_RING_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun thumbnailCell_notSelecting_showsNoRing() {
        composeRule.setContent { ThumbnailCell(thumbnail, "IMG_1.jpg", sizePx = 256) }

        composeRule.onNodeWithTag(SELECTION_RING_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun thumbnailCell_imageFails_showsBrokenImageIcon() {
        composeRule.setContent { ThumbnailCell(thumbnail, "IMG_1.jpg", sizePx = 256) }

        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodesWithTag(THUMBNAIL_FAILED_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun thumbnailCell_greyedOut_explainsWhy() {
        composeRule.setContent { ThumbnailCell(thumbnail, "IMG_1.jpg", sizePx = 256, greyedOut = true) }

        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not shown in this view"),
        ).assertIsDisplayed()
    }

    @Test
    fun thumbnailCell_badge_isShown() {
        composeRule.setContent {
            ThumbnailCell(thumbnail, "IMG_1.jpg", sizePx = 256, badge = { Text("badge") })
        }

        composeRule.onNodeWithText("badge", useUnmergedTree = true).assertIsDisplayed()
    }
}
