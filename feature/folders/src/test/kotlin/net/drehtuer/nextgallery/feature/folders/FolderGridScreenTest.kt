package net.drehtuer.nextgallery.feature.folders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderGridScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<FolderGridEvent>()

    private fun show(
        state: FolderGridUiState = FolderGridUiState(title = "Camera"),
        items: List<MediaItem> = listOf(mediaItem(1)),
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
        onBack: () -> Unit = {},
        onOpenSelectionOverview: () -> Unit = {},
    ) {
        val finished = LoadState.NotLoading(endOfPaginationReached = true)
        val pages = flowOf(PagingData.from(items, LoadStates(refresh, finished, finished)))
        composeRule.setContent {
            FolderGridScreen(state, pages.collectAsLazyPagingItems(), { events += it }, onBack, onOpenSelectionOverview)
        }
    }

    @Test
    fun noSelection_showsFolderName() {
        show()

        composeRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun backClicked_invokesCallback() {
        var backs = 0
        show(onBack = { backs++ })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun noItems_showsEmptyMessage() {
        show(items = emptyList())

        composeRule.onNodeWithText("This folder is empty").assertIsDisplayed()
    }

    @Test
    fun noItemsWhileLoading_showsLoadingIndicator() {
        show(items = emptyList(), refresh = LoadState.Loading)

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun loadFailed_showsError() {
        show(items = emptyList(), refresh = LoadState.Error(IllegalStateException()))

        composeRule.onNodeWithText("The photos could not be loaded").assertIsDisplayed()
    }

    @Test
    fun itemClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performClick()

        assertEquals(listOf(FolderGridEvent.ItemClicked(MediaId(1))), events)
    }

    @Test
    fun itemLongPressed_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performTouchInput { longClick() }

        assertEquals(listOf(FolderGridEvent.ItemLongPressed(MediaId(1))), events)
    }

    @Test
    fun selection_showsSelectionCountInsteadOfFolderName() {
        show(FolderGridUiState(title = "Camera", selectedIds = setOf(MediaId(1), MediaId(2))))

        composeRule.onNodeWithText("2 selected").assertIsDisplayed()
    }

    @Test
    fun selectAllClicked_sendsEvent() {
        show(FolderGridUiState(selectedIds = setOf(MediaId(1))))

        composeRule.onNodeWithContentDescription("Select all").performClick()

        assertEquals(listOf(FolderGridEvent.SelectAll), events)
    }

    @Test
    fun shareClicked_sendsEvent() {
        show(FolderGridUiState(selectedIds = setOf(MediaId(1))))

        composeRule.onNodeWithContentDescription("Share").performClick()

        assertEquals(listOf(FolderGridEvent.ShareSelected), events)
    }

    @Test
    fun deleteClicked_sendsEvent() {
        show(FolderGridUiState(selectedIds = setOf(MediaId(1))))

        composeRule.onNodeWithContentDescription("Delete").performClick()

        assertEquals(listOf(FolderGridEvent.DeleteSelected), events)
    }

    @Test
    fun selectionOverviewClicked_invokesCallback() {
        var opened = 0
        show(FolderGridUiState(selectedIds = setOf(MediaId(1))), onOpenSelectionOverview = { opened++ })

        composeRule.onNodeWithContentDescription("Show selection").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun clearSelectionClicked_sendsEvent() {
        show(FolderGridUiState(selectedIds = setOf(MediaId(1))))

        composeRule.onNodeWithContentDescription("Clear selection").performClick()

        assertEquals(listOf(FolderGridEvent.ClearSelection), events)
    }
}
