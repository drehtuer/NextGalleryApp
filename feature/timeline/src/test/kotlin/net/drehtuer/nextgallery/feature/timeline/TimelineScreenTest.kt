package net.drehtuer.nextgallery.feature.timeline

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import net.drehtuer.nextgallery.core.data.timeline.TimelineIndex
import net.drehtuer.nextgallery.core.model.DayCount
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.UploadAvailability
import net.drehtuer.nextgallery.core.model.UploadState
import net.drehtuer.nextgallery.core.ui.component.MEDIA_GRID_PLACEHOLDER_TAG
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TimelineScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<TimelineEvent>()
    private val visibleReports = mutableListOf<TimelineEvent.VisibleItemsChanged>()

    /** The grid reports what it shows by itself, at a time of its own. */
    private fun record(event: TimelineEvent) {
        if (event is TimelineEvent.VisibleItemsChanged) visibleReports += event else events += event
    }
    private val march16 = LocalDate.of(2024, 3, 16)
    private val onePhoto = TimelineIndex(listOf(DayCount(march16, 1)))

    private fun show(
        state: TimelineUiState = TimelineUiState(index = onePhoto),
        items: List<MediaItem> = listOf(mediaItem(1)),
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
        onOpenSelectionOverview: () -> Unit = {},
    ) {
        val finished = LoadState.NotLoading(endOfPaginationReached = true)
        val pages = flowOf(PagingData.from(items, LoadStates(refresh, finished, finished)))
        composeRule.setContent {
            TimelineScreen(state, pages.collectAsLazyPagingItems(), ::record, onOpenSelectionOverview)
        }
    }

    private val selecting = TimelineUiState(index = onePhoto, selectedIds = setOf(MediaId(1)))

    @Test
    fun uploadAvailable_clicked_sendsEvent() {
        show(selecting.copy(uploadAvailability = UploadAvailability.AVAILABLE))

        composeRule.onNodeWithContentDescription("Upload").performClick()

        assertEquals(listOf(TimelineEvent.UploadSelected), events)
    }

    @Test
    fun loggedOut_noUploadAction() {
        show(selecting.copy(uploadAvailability = UploadAvailability.HIDDEN))

        composeRule.onNodeWithContentDescription("Upload").assertDoesNotExist()
    }

    @Test
    fun badgeKnown_isShown() {
        show(TimelineUiState(index = onePhoto, uploadBadges = mapOf(MediaId(1) to UploadState.NotUploaded)))

        composeRule.onNodeWithContentDescription("Not on your Nextcloud", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun noBadgeEntry_showsNoBadge() {
        show()

        composeRule.onNodeWithContentDescription("Not on your Nextcloud", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun grid_reportsOnlyTheImagesAsVisible() {
        show()

        composeRule.waitUntil { visibleReports.isNotEmpty() }

        assertEquals(listOf(TimelineEvent.VisibleItemsChanged(setOf(MediaId(1)))), visibleReports)
    }

    @Test
    fun noSelection_showsTitle() {
        show()

        composeRule.onNodeWithText("Timeline").assertIsDisplayed()
    }

    @Test
    fun indexNotKnown_showsPlaceholderGrid() {
        show(TimelineUiState(index = null), items = emptyList(), refresh = LoadState.Loading)

        composeRule.onNodeWithTag(MEDIA_GRID_PLACEHOLDER_TAG).assertIsDisplayed()
    }

    @Test
    fun noImages_showsEmptyMessage() {
        show(TimelineUiState(index = TimelineIndex(emptyList())), items = emptyList())

        composeRule.onNodeWithText("No photos yet").assertIsDisplayed()
    }

    @Test
    fun loadFailed_showsError() {
        show(items = emptyList(), refresh = LoadState.Error(IllegalStateException()))

        composeRule.onNodeWithText("Couldn't load the timeline").assertIsDisplayed()
    }

    @Test
    fun stickyHeader_showsMonth() {
        show()

        composeRule.onNodeWithTag(STICKY_MONTH_TAG).assertTextEquals("March 2024")
    }

    @Test
    fun stickyHeader_afterScrolling_showsMonthOfFirstVisibleRow() {
        val february = LocalDate.of(2024, 2, 1)
        val index = TimelineIndex(listOf(DayCount(march16, 30), DayCount(february, 30)))
        show(TimelineUiState(index = index), items = List(60) { mediaItem(it.toLong()) })

        composeRule.onNodeWithTag(TIMELINE_GRID_TAG).performScrollToIndex(index.rowCount - 1)

        composeRule.onNodeWithTag(STICKY_MONTH_TAG).assertTextEquals("February 2024")
    }

    @Test
    fun monthHeader_isAlsoARowOfTheGrid() {
        show()

        assertEquals(2, composeRule.onAllNodesWithText("March 2024").fetchSemanticsNodes().size)
    }

    @Test
    fun dayHeader_showsWeekdayDayAndMonth() {
        show()

        composeRule.onNodeWithText("Saturday 16 March").assertIsDisplayed()
    }

    @Test
    fun dayHeader_today_startsWithToday() {
        val today = LocalDate.of(2026, 11, 6)
        show(TimelineUiState(index = TimelineIndex(listOf(DayCount(today, 1))), today = today))

        composeRule.onNodeWithText("Today · Friday 6 November").assertIsDisplayed()
    }

    @Test
    fun dayHeader_otherDay_doesNotSayToday() {
        show(TimelineUiState(index = onePhoto, today = march16.plusDays(1)))

        composeRule.onNodeWithText("Today", substring = true).assertDoesNotExist()
    }

    @Test
    fun photoNotInPagedListYet_showsPlaceholderInsteadOfCrashing() {
        show(items = emptyList())

        composeRule.onNodeWithText("Saturday 16 March").assertIsDisplayed()
    }

    @Test
    fun itemClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performClick()

        assertEquals(listOf(TimelineEvent.ItemClicked(MediaId(1))), events)
    }

    @Test
    fun itemLongPressed_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performTouchInput { longClick() }

        assertEquals(listOf(TimelineEvent.ItemLongPressed(MediaId(1))), events)
    }

    @Test
    fun selection_showsSelectionCountInsteadOfTitle() {
        show(TimelineUiState(index = onePhoto, selectedIds = setOf(MediaId(1), MediaId(2))))

        composeRule.onNodeWithText("2 selected").assertIsDisplayed()
    }

    @Test
    fun selectClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("Select").performClick()

        assertEquals(listOf(TimelineEvent.StartSelecting), events)
    }

    @Test
    fun selectAllClicked_sendsEvent() {
        show(TimelineUiState(index = onePhoto, selecting = true))

        composeRule.onNodeWithContentDescription("Select all").performClick()

        assertEquals(listOf(TimelineEvent.SelectAll), events)
    }

    @Test
    fun shareClicked_sendsEvent() {
        show(TimelineUiState(index = onePhoto, selecting = true))

        composeRule.onNodeWithContentDescription("Share").performClick()

        assertEquals(listOf(TimelineEvent.ShareSelected), events)
    }

    @Test
    fun deleteClicked_sendsEvent() {
        show(TimelineUiState(index = onePhoto, selecting = true))

        composeRule.onNodeWithContentDescription("Delete").performClick()

        assertEquals(listOf(TimelineEvent.DeleteSelected), events)
    }

    @Test
    fun selectionOverviewClicked_invokesCallback() {
        var opened = 0
        show(TimelineUiState(index = onePhoto, selecting = true), onOpenSelectionOverview = { opened++ })

        composeRule.onNodeWithContentDescription("Show selection").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun clearSelectionClicked_sendsEvent() {
        show(TimelineUiState(index = onePhoto, selecting = true))

        composeRule.onNodeWithContentDescription("Clear selection").performClick()

        assertEquals(listOf(TimelineEvent.ClearSelection), events)
    }

    @Test
    fun backDuringSelection_clearsSelection() {
        var pressBack: () -> Unit = {}
        val pages = flowOf(PagingData.from(listOf(mediaItem(1))))
        composeRule.setContent {
            val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            pressBack = { dispatcher?.onBackPressed() }
            TimelineScreen(
                TimelineUiState(index = onePhoto, selecting = true),
                pages.collectAsLazyPagingItems(),
                ::record,
                {},
            )
        }

        composeRule.runOnIdle { pressBack() }

        assertEquals(listOf(TimelineEvent.ClearSelection), events)
    }
}
