package net.drehtuer.nextgallery.feature.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.drehtuer.nextgallery.core.data.timeline.TimelineIndex
import net.drehtuer.nextgallery.core.model.DayCount
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * A 300 dp grid with rows of 100 dp, so the middle (150 dp) is always in the second visible row.
 * With one image per day, the rows are: 0 month, 1 day 1, 2 image, 3 day 2, 4 image, 5 day 3, 6 image ...
 */
@RunWith(AndroidJUnit4::class)
class DateRailTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var gridState: LazyGridState

    private fun day(number: Int) = DayCount(LocalDate.of(2026, 10, 25 - number), 1)

    private val threeDays = TimelineIndex(listOf(day(1), day(2), day(3)))

    private fun show(index: TimelineIndex = threeDays) {
        composeRule.setContent {
            gridState = rememberLazyGridState()
            Box(Modifier.size(300.dp)) {
                LazyVerticalGrid(GridCells.Fixed(1), state = gridState) {
                    items(index.rowCount) { Box(Modifier.fillMaxWidth().height(100.dp)) }
                }
                DateRail(gridState, index)
            }
        }
    }

    private fun scrollTo(row: Int) = composeRule.runOnIdle { runBlocking { gridState.scrollToItem(row) } }

    @Test
    fun current_isTheDayInTheMiddleNotTheFirstVisibleOne() {
        show()

        scrollTo(2)

        composeRule.onNodeWithTag(DATE_RAIL_CURRENT_TAG).assertTextEquals("23 Oct 2026")
    }

    @Test
    fun above_isTheNewerNeighbour() {
        show()

        scrollTo(2)

        composeRule.onNodeWithTag(DATE_RAIL_ABOVE_TAG).assertTextEquals("24 Oct 2026")
    }

    @Test
    fun below_isTheOlderNeighbour() {
        show()

        scrollTo(2)

        composeRule.onNodeWithTag(DATE_RAIL_BELOW_TAG).assertTextEquals("22 Oct 2026")
    }

    @Test
    fun startOfTimeline_leavesOutTheDayAbove() {
        show()

        composeRule.onNodeWithTag(DATE_RAIL_ABOVE_TAG).assertDoesNotExist()
    }

    @Test
    fun endOfTimeline_leavesOutTheDayBelow() {
        show()

        scrollTo(threeDays.rowCount - 1)

        composeRule.onNodeWithTag(DATE_RAIL_BELOW_TAG).assertDoesNotExist()
    }

    @Test
    fun noRows_showsNoDates() {
        show(TimelineIndex(emptyList()))

        composeRule.onNodeWithTag(DATE_RAIL_CURRENT_TAG).assertDoesNotExist()
    }

    @Test
    fun thumb_atStart_isAtTheTop() {
        show()

        composeRule.onNodeWithTag(DATE_RAIL_THUMB_TAG).assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun thumb_atEnd_isAtTheBottom() {
        show()

        scrollTo(threeDays.rowCount - 1)

        composeRule.onNodeWithTag(DATE_RAIL_THUMB_TAG).assertTopPositionInRootIsEqualTo(252.dp)
    }

    @Test
    fun draggingThumbToTheBottom_scrollsToTheEnd() {
        val index = TimelineIndex((1..20).map { day(it) })
        show(index)

        composeRule.onNodeWithTag(DATE_RAIL_THUMB_TAG).performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 300.dp.toPx())
        }

        composeRule.runOnIdle {
            assertEquals(index.rowCount - gridState.visibleCount(), gridState.firstVisibleItemIndex)
        }
    }

    private fun LazyGridState.visibleCount() = layoutInfo.visibleItemsInfo.size

    private val twentyDays = TimelineIndex((1..20).map { day(it) })

    @Test
    fun thumb_isNamedForAScreenReader() {
        show()

        composeRule.onNodeWithTag(DATE_RAIL_THUMB_TAG).assertContentDescriptionEquals("Scroll position")
    }

    @Test
    fun thumb_saysTheDateInTheMiddle() {
        show()

        scrollTo(2)

        composeRule.onNodeWithTag(DATE_RAIL_THUMB_TAG).assert(hasStateDescription("23 Oct 2026"))
    }

    @Test
    fun thumb_reportsTheScrollPosition() {
        show(twentyDays)

        composeRule.runOnIdle { runBlocking { gridState.scrollToItem(twentyDays.rowCount - 1) } }

        composeRule.onNodeWithTag(DATE_RAIL_THUMB_TAG).assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun setProgress_jumpsToThatPositionWithoutDragging() {
        show(twentyDays)

        composeRule.onNodeWithTag(DATE_RAIL_THUMB_TAG).performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }

        composeRule.runOnIdle {
            assertEquals(twentyDays.rowCount - gridState.visibleCount(), gridState.firstVisibleItemIndex)
        }
    }

    @Test
    fun labels_areLeftToTheThumbForAScreenReader() {
        show()

        composeRule.onNode(isHiddenFromAccessibility).assertExists()
    }
}

private val isHiddenFromAccessibility = SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility)
