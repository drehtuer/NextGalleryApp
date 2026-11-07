package net.drehtuer.nextgallery.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class MapScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<MapEvent>()
    private var overviewOpened = 0

    private fun show(state: MapUiState = MapUiState()) {
        val pages = flowOf(PagingData.empty<MediaItem>())
        composeRule.setContent {
            MapScreen(
                state = state,
                sheetItems = pages.collectAsLazyPagingItems(),
                onEvent = { events += it },
                onOpenSelectionOverview = { overviewOpened++ },
                map = { Box(it.then(Modifier.testTag("map"))) },
            )
        }
    }

    @Test
    fun map_isShown() {
        show()

        composeRule.onNodeWithTag("map").assertIsDisplayed()
    }

    @Test
    fun attribution_isShown() {
        show()

        composeRule.onNodeWithText("© OpenStreetMap contributors").assertIsDisplayed()
    }

    @Test
    fun zoomInClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("Zoom in").performClick()

        assertEquals(listOf<MapEvent>(MapEvent.ZoomIn), events)
    }

    @Test
    fun zoomOutClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("Zoom out").performClick()

        assertEquals(listOf<MapEvent>(MapEvent.ZoomOut), events)
    }

    @Test
    fun myLocationClicked_sendsEvent() {
        show()

        composeRule.onNodeWithContentDescription("Show my location").performClick()

        assertEquals(listOf<MapEvent>(MapEvent.ShowMyLocation), events)
    }

    @Test
    fun photoLocationCard_showsDesignWording() {
        show(MapUiState(showPhotoLocationCard = true))

        composeRule.onNodeWithText("See where your photos were taken").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Android hides the location in photos until you allow it. " +
                "NextGallery only reads it to place photos on the map.",
        ).assertIsDisplayed()
    }

    @Test
    fun photoLocationCard_hidden_isNotShown() {
        show()

        composeRule.onNodeWithText("See where your photos were taken").assertDoesNotExist()
    }

    @Test
    fun allowClicked_sendsEvent() {
        show(MapUiState(showPhotoLocationCard = true))

        composeRule.onNodeWithText("Allow").performClick()

        assertEquals(listOf<MapEvent>(MapEvent.AllowPhotoLocations), events)
    }

    @Test
    fun notNowClicked_sendsEvent() {
        show(MapUiState(showPhotoLocationCard = true))

        composeRule.onNodeWithText("Not now").performClick()

        assertEquals(listOf<MapEvent>(MapEvent.DismissPhotoLocations), events)
    }

    @Test
    fun noGeotaggedCard_showsDesignWording() {
        show(MapUiState(showNoGeotaggedCard = true))

        composeRule.onNodeWithText("No geotagged images").assertIsDisplayed()
        composeRule.onNodeWithText(
            "None of your photos has a location saved. Turn on location in your camera app to see new photos here.",
        ).assertIsDisplayed()
    }

    @Test
    fun noGeotaggedCard_hidden_isNotShown() {
        show()

        composeRule.onNodeWithText("No geotagged images").assertDoesNotExist()
    }

    @Test
    fun selection_showsCountBar() {
        show(MapUiState(selectedIds = setOf(MediaId(1), MediaId(2))))

        composeRule.onNodeWithText("2 selected").assertIsDisplayed()
    }

    @Test
    fun noSelection_hidesBar() {
        show()

        composeRule.onNodeWithContentDescription("Clear selection").assertDoesNotExist()
    }

    @Test
    fun selectionClearClicked_sendsEvent() {
        show(MapUiState(selectedIds = setOf(MediaId(1))))

        composeRule.onNodeWithContentDescription("Clear selection").performClick()

        assertEquals(listOf<MapEvent>(MapSelectionEvent.Clear), events)
    }

    @Test
    fun selectionOverviewClicked_opensOverview() {
        show(MapUiState(selectedIds = setOf(MediaId(1))))

        composeRule.onNodeWithContentDescription("Show selection").performClick()

        assertEquals(1, overviewOpened)
    }

    @Test
    fun selectionShareClicked_sendsEvent() {
        show(MapUiState(selectedIds = setOf(MediaId(1))))

        composeRule.onNodeWithContentDescription("Share").performClick()

        assertEquals(listOf<MapEvent>(MapSelectionEvent.Share), events)
    }

    @Test
    fun selectionDeleteClicked_sendsEvent() {
        show(MapUiState(selectedIds = setOf(MediaId(1))))

        composeRule.onNodeWithContentDescription("Delete").performClick()

        assertEquals(listOf<MapEvent>(MapSelectionEvent.Delete), events)
    }

    @Test
    fun openCluster_showsItsSheet() {
        show(MapUiState(openCluster = mapCluster(128)))

        composeRule.onNodeWithText("Near Bolzano").assertIsDisplayed()
    }
}
