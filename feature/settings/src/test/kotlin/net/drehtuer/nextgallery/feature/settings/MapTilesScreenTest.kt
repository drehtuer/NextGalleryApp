package net.drehtuer.nextgallery.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapTilesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val validUrl = "https://tiles.example.org/{z}/{x}/{y}.png"
    private val events = mutableListOf<MapTilesEvent>()
    private var backs = 0

    private fun show(state: MapTilesUiState) = composeRule.setContent {
        MapTilesScreen(state, { events += it }, { backs++ })
    }

    @Test
    fun field_showsAddress() {
        show(MapTilesUiState(validUrl))

        composeRule.onNodeWithText(validUrl).assertIsDisplayed()
    }

    @Test
    fun invalidAddress_showsError() {
        show(MapTilesUiState("nonsense", edited = true))

        composeRule.onNodeWithText("The address must start with https:// and contain {z}, {x} and {y}.")
            .assertIsDisplayed()
    }

    @Test
    fun validAddress_hidesError() {
        show(MapTilesUiState(validUrl, edited = true))

        composeRule.onNodeWithText("The address must start with https:// and contain {z}, {x} and {y}.")
            .assertDoesNotExist()
    }

    @Test
    fun noEdit_saveIsDisabled() {
        show(MapTilesUiState(validUrl))

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun invalidEdit_saveIsDisabled() {
        show(MapTilesUiState("nonsense", edited = true))

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveClicked_sendsEvent() {
        show(MapTilesUiState(validUrl, edited = true))

        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()

        assertEquals(listOf<MapTilesEvent>(MapTilesEvent.Save), events)
    }

    @Test
    fun useOpenStreetMapClicked_sendsEvent() {
        show(MapTilesUiState(validUrl))

        composeRule.onNodeWithText("Use OpenStreetMap").performClick()

        assertEquals(listOf<MapTilesEvent>(MapTilesEvent.UseDefault), events)
    }

    @Test
    fun typing_sendsEvent() {
        show(MapTilesUiState(validUrl))

        composeRule.onNodeWithText(validUrl).performTextReplacement("https://other")

        assertEquals(listOf<MapTilesEvent>(MapTilesEvent.UrlChanged("https://other")), events)
    }

    @Test
    fun backClicked_goesBack() {
        show(MapTilesUiState(validUrl))

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
