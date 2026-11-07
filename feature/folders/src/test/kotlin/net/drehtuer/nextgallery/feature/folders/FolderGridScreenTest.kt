package net.drehtuer.nextgallery.feature.folders

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.UploadAvailability
import net.drehtuer.nextgallery.core.model.UploadState
import net.drehtuer.nextgallery.core.ui.component.MEDIA_GRID_PLACEHOLDER_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderGridScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<FolderGridEvent>()
    private val visibleReports = mutableListOf<FolderGridEvent.VisibleItemsChanged>()

    /** The grid reports what it shows by itself, at a time of its own. */
    private fun record(event: FolderGridEvent) {
        if (event is FolderGridEvent.VisibleItemsChanged) visibleReports += event else events += event
    }

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
            FolderGridScreen(state, pages.collectAsLazyPagingItems(), ::record, onBack, onOpenSelectionOverview)
        }
    }

    private val selecting = FolderGridUiState(selectedIds = setOf(MediaId(1)))

    @Test
    fun uploadAvailable_clicked_sendsEvent() {
        show(selecting.copy(uploadAvailability = UploadAvailability.AVAILABLE))

        composeRule.onNodeWithContentDescription("Upload").performClick()

        assertEquals(listOf(FolderGridEvent.UploadSelected), events)
    }

    @Test
    fun loggedOut_noUploadAction() {
        show(selecting.copy(uploadAvailability = UploadAvailability.HIDDEN))

        composeRule.onNodeWithContentDescription("Upload").assertDoesNotExist()
    }

    @Test
    fun badgeKnown_isShown() {
        show(FolderGridUiState(uploadBadges = mapOf(MediaId(1) to UploadState.NotUploaded)))

        composeRule.onNodeWithContentDescription("Not on your Nextcloud", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun noBadgeEntry_showsNoBadge() {
        show(FolderGridUiState())

        composeRule.onNodeWithContentDescription("Not on your Nextcloud", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun grid_reportsTheVisibleImages() {
        show()

        composeRule.waitUntil { visibleReports.isNotEmpty() }

        assertEquals(listOf(FolderGridEvent.VisibleItemsChanged(setOf(MediaId(1)))), visibleReports)
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
    fun noItemsWhileLoading_showsPlaceholderGrid() {
        show(items = emptyList(), refresh = LoadState.Loading)

        composeRule.onNodeWithTag(MEDIA_GRID_PLACEHOLDER_TAG).assertIsDisplayed()
    }

    @Test
    fun loadFailed_showsError() {
        show(items = emptyList(), refresh = LoadState.Error(IllegalStateException()))

        composeRule.onNodeWithText("Couldn't load this folder").assertIsDisplayed()
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
    fun selectClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("Select").performClick()

        assertEquals(listOf(FolderGridEvent.StartSelecting), events)
    }

    @Test
    fun selectingWithoutImages_showsSelectionBar() {
        show(FolderGridUiState(selecting = true))

        composeRule.onNodeWithText("0 selected").assertIsDisplayed()
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

    @Test
    fun selecting_actionsAllHaveANameAndATouchTarget() {
        show(selecting.copy(uploadAvailability = UploadAvailability.AVAILABLE))

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
