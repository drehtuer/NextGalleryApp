package net.drehtuer.nextgallery.feature.folders

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
class FolderListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: FolderListUiState, onFolderClick: (Long) -> Unit = {}) =
        composeRule.setContent { FolderListScreen(state, onFolderClick) }

    @Test
    fun loading_showsLoadingIndicator() {
        show(FolderListUiState.Loading)

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun empty_showsMessage() {
        show(FolderListUiState.Empty)

        composeRule.onNodeWithText("No photos found").assertIsDisplayed()
    }

    @Test
    fun content_showsNameAndCount() {
        show(FolderListUiState.Content(listOf(folder(name = "Camera", itemCount = 12))))

        composeRule.onNodeWithText("12 images").assertIsDisplayed()
    }

    @Test
    fun folderClicked_reportsBucket() {
        var clicked: Long? = null
        show(FolderListUiState.Content(listOf(folder(bucketId = 8, name = "Screenshots")))) { clicked = it }

        composeRule.onNodeWithText("Screenshots").performClick()

        assertEquals(8L, clicked)
    }
}
