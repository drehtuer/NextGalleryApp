package net.drehtuer.nextgallery.core.ui.component

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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 100 rows of 100 dp in a 300 dp grid: 3 are visible, so the last first-visible row is 97. */
@RunWith(AndroidJUnit4::class)
class FastScrollbarAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var gridState: LazyGridState

    private fun show(label: (Int) -> String? = { null }) {
        composeRule.setContent {
            gridState = rememberLazyGridState()
            Box(Modifier.size(300.dp)) {
                LazyVerticalGrid(GridCells.Fixed(1), state = gridState) {
                    items(ROWS) { Box(Modifier.fillMaxWidth().height(100.dp)) }
                }
                FastScrollbar(gridState, ROWS, label = label)
            }
        }
    }

    private fun thumb() = composeRule.onNodeWithTag(FAST_SCROLLBAR_THUMB_TAG)

    @Test
    fun thumb_isNamed() {
        show()

        thumb().assertContentDescriptionEquals("Scroll position")
    }

    @Test
    fun thumb_isALargeEnoughTouchTarget() {
        show()

        thumb().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun thumb_atTheStart_reportsZero() {
        show()

        thumb().assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
    }

    @Test
    fun thumb_followsScrolling() {
        show()

        composeRule.runOnIdle { runBlocking { gridState.scrollToItem(ROWS - 1) } }

        thumb().assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun thumb_withLabel_saysTheLabelInsteadOfAPercentage() {
        show(label = { "row $it" })

        thumb().assert(hasStateDescription("row 0"))
    }

    @Test
    fun setProgress_jumpsToThatPositionWithoutDragging() {
        show()

        thumb().performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }

        composeRule.runOnIdle { assertEquals(ROWS - VISIBLE_ROWS, gridState.firstVisibleItemIndex) }
    }

    @Test
    fun setProgress_outOfRange_staysAtTheEnd() {
        show()

        thumb().performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }

        composeRule.runOnIdle { assertEquals(ROWS - VISIBLE_ROWS, gridState.firstVisibleItemIndex) }
    }

    private companion object {
        const val ROWS = 100
        const val VISIBLE_ROWS = 3
    }
}
