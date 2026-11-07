package net.drehtuer.nextgallery.feature.map

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
import net.drehtuer.nextgallery.core.model.MapCluster
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClusterSheetContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<MapEvent>()

    private fun show(cluster: MapCluster = mapCluster(128), selectedIds: Set<MediaId> = emptySet()) {
        val finished = LoadState.NotLoading(endOfPaginationReached = true)
        val pages = flowOf(PagingData.from(listOf(mediaItem(1)), LoadStates(finished, finished, finished)))
        composeRule.setContent {
            ClusterSheetContent(cluster, pages.collectAsLazyPagingItems(), selectedIds, { events += it }, {})
        }
    }

    @Test
    fun withTown_showsTitleAndCount() {
        show()

        composeRule.onNodeWithText("Near Bolzano").assertIsDisplayed()
        composeRule.onNodeWithText("128 images").assertIsDisplayed()
    }

    @Test
    fun withoutTown_showsCountAndCoordinates() {
        show(mapCluster(4, title = null))

        composeRule.onNodeWithText("4 images").assertIsDisplayed()
        composeRule.onNodeWithText("46.50° N, 11.30° E").assertIsDisplayed()
    }

    @Test
    fun itemClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performClick()

        assertEquals(listOf<MapEvent>(MapSelectionEvent.ItemClicked(MediaId(1))), events)
    }

    @Test
    fun itemLongPressed_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performTouchInput { longClick() }

        assertEquals(listOf<MapEvent>(MapSelectionEvent.ItemLongPressed(MediaId(1))), events)
    }

    @Test
    fun duringSelection_barReplacesTitle() {
        show(selectedIds = setOf(MediaId(1)))

        composeRule.onNodeWithText("1 selected").assertIsDisplayed()
        composeRule.onNodeWithText("Near Bolzano").assertDoesNotExist()
    }
}
