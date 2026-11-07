package net.drehtuer.nextgallery.feature.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: AppearanceUiState = AppearanceUiState(), onEvent: (AppearanceEvent) -> Unit = {}) =
        composeRule.setContent { AppearanceScreen(state, onEvent, onBack = {}) }

    @Test
    fun theme_marksTheCurrentMode() {
        show(AppearanceUiState(themeMode = ThemeMode.DARK))

        composeRule.onNodeWithText("Dark").assertIsSelected()
    }

    @Test
    fun theme_showsNoteAboutViewer() {
        show()

        composeRule.onNodeWithText("The photo viewer and the editor stay black in every theme.").assertIsDisplayed()
    }

    @Test
    fun themeClicked_reportsMode() {
        var event: AppearanceEvent? = null
        show(onEvent = { event = it })

        composeRule.onNodeWithText("Light").performClick()

        assertEquals(AppearanceEvent.ThemeModeSelected(ThemeMode.LIGHT), event)
    }

    @Test
    fun columnsClicked_reportsColumns() {
        var event: AppearanceEvent? = null
        show(onEvent = { event = it })

        composeRule.onNodeWithText("4").performClick()

        assertEquals(AppearanceEvent.GridColumnsSelected(4), event)
    }

    @Test
    fun preview_showsTwoRowsOfTheChosenColumns() {
        show(AppearanceUiState(gridColumns = 5))

        composeRule.onAllNodesWithTag(COLUMN_PREVIEW_CELL_TAG, useUnmergedTree = true).assertCountEquals(10)
    }

    @Test
    fun accentNote_namesTheCurrentAccent() {
        show(AppearanceUiState(accentColor = AccentColor.TEAL))

        composeRule
            .onNodeWithText(
                "Teal. Colours buttons, selections and the scrollbar. Warning and error colours never change.",
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun accentSwatch_marksTheCurrentAccent() {
        show(AppearanceUiState(accentColor = AccentColor.CORAL))

        composeRule.onNodeWithContentDescription("Coral").assertIsSelected()
        composeRule.onNodeWithContentDescription("Blue").assertIsNotSelected()
    }

    @Test
    fun accentSwatch_onlyTheCurrentAccentHasACheck() {
        show(AppearanceUiState(accentColor = AccentColor.CORAL))

        composeRule.onAllNodesWithTag(ACCENT_CHECK_TAG, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun accentSwatchClicked_reportsAccent() {
        var event: AppearanceEvent? = null
        show(onEvent = { event = it })

        composeRule.onNodeWithContentDescription("Moss").performScrollTo().performClick()

        assertEquals(AppearanceEvent.AccentSelected(AccentColor.MOSS), event)
    }
}
