package net.drehtuer.nextgallery.feature.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.flowOf
import net.drehtuer.nextgallery.core.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ViewerEvent>()

    private fun show(
        state: ViewerUiState = ViewerUiState(initialIndex = 0, current = mediaItem(1)),
        items: List<MediaItem> = listOf(mediaItem(1), mediaItem(2)),
        onBack: () -> Unit = {},
    ) {
        val finished = LoadState.NotLoading(endOfPaginationReached = true)
        val pages = flowOf(PagingData.from(items, LoadStates(finished, finished, finished)))
        composeRule.setContent {
            ViewerScreen(
                state = state,
                items = pages.collectAsLazyPagingItems(),
                onEvent = { events += it },
                onBack = onBack,
                pageContent = { item, onTap ->
                    Text("page ${item.id.value}", Modifier.fillMaxSize().clickable(onClick = onTap))
                },
            )
        }
    }

    @Test
    fun initialIndex_showsThatImage() {
        show(ViewerUiState(initialIndex = 1))

        composeRule.onNodeWithText("page 2").assertIsDisplayed()
    }

    @Test
    fun positionUnknown_showsLoadingIndicator() {
        show(ViewerUiState(initialIndex = null))

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun swipe_reportsNextImage() {
        show()

        composeRule.onNodeWithText("page 1").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(ViewerEvent.PageChanged(mediaItem(2)), events.filterIsInstance<ViewerEvent.PageChanged>().last())
    }

    @Test
    fun tap_togglesChrome() {
        show()

        composeRule.onNodeWithText("page 1").performClick()

        assertTrue(ViewerEvent.ToggleChrome in events)
    }

    @Test
    fun chromeVisible_showsImageName() {
        show()

        composeRule.onNodeWithText("IMG_1.jpg").assertIsDisplayed()
    }

    @Test
    fun chromeHidden_hidesControls() {
        show(ViewerUiState(initialIndex = 0, chromeVisible = false))

        composeRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    @Test
    fun backClicked_invokesCallback() {
        var backs = 0
        show(onBack = { backs++ })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun shareClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("Share").performClick()

        assertTrue(ViewerEvent.Share in events)
    }

    @Test
    fun deleteClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("Delete").performClick()

        assertTrue(ViewerEvent.Delete in events)
    }

    @Test
    fun infoClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("Image info").performClick()

        assertTrue(ViewerEvent.ShowInfo in events)
    }

    @Test
    fun itemsLoaded_reportsCount() {
        show()
        composeRule.waitForIdle()

        assertTrue(ViewerEvent.ItemCountChanged(2) in events)
    }
}
