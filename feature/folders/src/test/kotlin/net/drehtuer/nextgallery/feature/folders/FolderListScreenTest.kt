package net.drehtuer.nextgallery.feature.folders

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: FolderListUiState,
        onOpenSettings: () -> Unit = {},
        onOpenFolderSettings: () -> Unit = {},
        onVirtualFolderClick: (String) -> Unit = {},
        onFolderClick: (Long) -> Unit = {},
    ) = composeRule.setContent {
        FolderListScreen(state, onFolderClick, onVirtualFolderClick, onOpenSettings, onOpenFolderSettings)
    }

    @Test
    fun loading_showsLoadingIndicator() {
        show(FolderListUiState.Loading)

        composeRule.onNodeWithTag(FOLDER_LIST_PLACEHOLDER_TAG).assertIsDisplayed()
    }

    @Test
    fun noFolders_showsMessage() {
        show(FolderListUiState.NoFolders)

        composeRule.onNodeWithText("No folders added").assertIsDisplayed()
    }

    @Test
    fun noFolders_addFolderClicked_opensFolderSettings() {
        var clicks = 0
        show(FolderListUiState.NoFolders, onOpenFolderSettings = { clicks++ })

        composeRule.onNodeWithText("Add folder").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun noImages_showsMessage() {
        show(FolderListUiState.NoImages)

        composeRule.onNodeWithText("No photos in your folders").assertIsDisplayed()
    }

    @Test
    fun noImages_manageFoldersClicked_opensFolderSettings() {
        var clicks = 0
        show(FolderListUiState.NoImages, onOpenFolderSettings = { clicks++ })

        composeRule.onNodeWithText("Manage folders").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun virtualFolder_isMarkedAsSuch() {
        show(FolderListUiState.Content(listOf(folder()), listOf(virtualFolder())))

        composeRule.onNodeWithContentDescription("Virtual folder", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun realFolder_isNotMarkedAsVirtual() {
        show(FolderListUiState.Content(listOf(folder())))

        composeRule.onNodeWithContentDescription("Virtual folder", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun virtualFolder_comesBeforeRealFolders() {
        show(FolderListUiState.Content(listOf(folder(name = "Camera")), listOf(virtualFolder(name = "Everything"))))

        val names = composeRule.onAllNodes(hasText("Everything") or hasText("Camera"))
            .fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.Text].first().text }

        assertEquals(listOf("Everything", "Camera"), names)
    }

    @Test
    fun virtualFolderClicked_reportsItsId() {
        var clicked: String? = null
        show(
            FolderListUiState.Content(listOf(folder()), listOf(virtualFolder(id = "v9"))),
            onVirtualFolderClick = { clicked = it },
        )

        composeRule.onNodeWithText("Everything").performClick()

        assertEquals("v9", clicked)
    }

    @Test
    fun content_showsNameAndCount() {
        show(FolderListUiState.Content(listOf(folder(name = "Camera", itemCount = 12))))

        composeRule.onNodeWithText("12 images").assertIsDisplayed()
    }

    @Test
    fun content_largeCount_usesThousandsSeparator() {
        show(FolderListUiState.Content(listOf(folder(itemCount = 1_204))))

        composeRule.onNodeWithText("1,204 images").assertIsDisplayed()
    }

    @Test
    fun folderClicked_reportsBucket() {
        var clicked: Long? = null
        show(FolderListUiState.Content(listOf(folder(bucketId = 8, name = "Screenshots")))) { clicked = it }

        composeRule.onNodeWithText("Screenshots").performClick()

        assertEquals(8L, clicked)
    }

    @Test
    fun settingsIconClicked_opensSettings() {
        var clicks = 0
        show(FolderListUiState.NoFolders, onOpenSettings = { clicks++ })

        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun actions_allHaveANameAndATouchTarget() {
        show(FolderListUiState.Content(listOf(folder(name = "Camera", itemCount = 12))))

        val actions = composeRule.onAllNodes(hasClickAction())

        assertTrue(actions.fetchSemanticsNodes().isNotEmpty())
        actions.assertAll(namedTouchTarget)
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
