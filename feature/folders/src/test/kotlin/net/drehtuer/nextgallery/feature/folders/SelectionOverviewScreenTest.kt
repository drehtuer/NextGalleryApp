package net.drehtuer.nextgallery.feature.folders

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.data.selection.SelectionOverviewItem
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.UploadAvailability
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectionOverviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SelectionOverviewEvent>()

    private fun show(items: List<SelectionOverviewItem>?, onBack: () -> Unit = {}) = composeRule.setContent {
        SelectionOverviewScreen(SelectionOverviewUiState(items), { events += it }, onBack)
    }

    private val shown = SelectionOverviewItem(mediaItem(1), greyedOut = false)

    private fun showWithUpload(availability: UploadAvailability) = composeRule.setContent {
        SelectionOverviewScreen(
            SelectionOverviewUiState(listOf(shown), uploadAvailability = availability),
            { events += it },
            onBack = {},
        )
    }

    @Test
    fun uploadClicked_sendsEvent() {
        showWithUpload(UploadAvailability.AVAILABLE)

        composeRule.onNodeWithContentDescription("Upload").performClick()

        assertEquals(listOf(SelectionOverviewEvent.Upload), events)
    }

    @Test
    fun loggedOut_noUploadAction() {
        showWithUpload(UploadAvailability.HIDDEN)

        composeRule.onNodeWithContentDescription("Upload").assertDoesNotExist()
    }

    @Test
    fun loading_showsLoadingIndicator() {
        show(null)

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun empty_showsMessage() {
        show(emptyList())

        composeRule.onNodeWithText("Nothing selected").assertIsDisplayed()
    }

    @Test
    fun empty_hidesActions() {
        show(emptyList())

        composeRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    @Test
    fun greyedOutImage_isMarked() {
        show(listOf(shown.copy(greyedOut = true)))

        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not shown in this view"),
        ).assertIsDisplayed()
    }

    @Test
    fun greyedOutImages_noteCountsThem() {
        show(listOf(shown, shown.copy(item = mediaItem(2), greyedOut = true)))

        composeRule.onNodeWithText(
            "2 selected in all folders. Greyed out: 1 in other folders, which this view can't open.",
        ).assertIsDisplayed()
    }

    @Test
    fun greyedOutOnMap_noteNamesTheMissingLocation() {
        composeRule.setContent {
            SelectionOverviewScreen(
                SelectionOverviewUiState(listOf(shown.copy(greyedOut = true)), greyedOutForMissingLocation = true),
                {},
                {},
            )
        }

        composeRule.onNodeWithText(
            "1 selected in all folders. Greyed out: 1 without a location, which the map can't show.",
        ).assertIsDisplayed()
    }

    @Test
    fun noGreyedOutImages_noteOnlyCounts() {
        show(listOf(shown))

        composeRule.onNodeWithText("1 selected in all folders.").assertIsDisplayed()
    }

    @Test
    fun itemClicked_sendsEvent() {
        show(listOf(shown))

        composeRule.onNodeWithContentDescription("IMG_1.jpg", substring = true).performClick()

        assertEquals(listOf(SelectionOverviewEvent.ItemClicked(MediaId(1))), events)
    }

    @Test
    fun shareClicked_sendsEvent() {
        show(listOf(shown))

        composeRule.onNodeWithContentDescription("Share").performClick()

        assertEquals(listOf(SelectionOverviewEvent.Share), events)
    }

    @Test
    fun deleteClicked_sendsEvent() {
        show(listOf(shown))

        composeRule.onNodeWithContentDescription("Delete").performClick()

        assertEquals(listOf(SelectionOverviewEvent.Delete), events)
    }

    @Test
    fun backClicked_invokesCallback() {
        var backs = 0
        show(emptyList()) { backs++ }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
