package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisibleMediaIdsEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val reports = mutableListOf<Set<MediaId>>()

    private fun show(keys: List<Any>) = composeRule.setContent {
        val state = rememberLazyGridState()
        LazyVerticalGrid(GridCells.Fixed(1), Modifier.size(100.dp), state) {
            items(keys.size, key = { keys[it] }) { Box(Modifier.size(10.dp)) }
        }
        VisibleMediaIdsEffect(state) { reports += it }
    }

    @Test
    fun reportsTheImagesOnScreen() {
        show(listOf(1L, 2L))

        composeRule.waitUntil { reports.isNotEmpty() }

        assertEquals(listOf(setOf(MediaId(1), MediaId(2))), reports)
    }

    @Test
    fun leavesOutCellsThatAreNoImages() {
        show(listOf("day-header", 2L))

        composeRule.waitUntil { reports.isNotEmpty() }

        assertEquals(listOf(setOf(MediaId(2))), reports)
    }

    @Test
    fun beforeTheDelay_reportsNothing() {
        composeRule.mainClock.autoAdvance = false
        show(listOf(1L))

        composeRule.mainClock.advanceTimeBy(VISIBLE_IDS_DEBOUNCE_MILLIS / 2)

        assertEquals(emptyList<Set<MediaId>>(), reports)
    }
}
