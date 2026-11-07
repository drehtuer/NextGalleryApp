package net.drehtuer.nextgallery.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderRulesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val row = FolderRuleRow("Camera", "DCIM/Camera", 1_204, RuleChoice.DEFAULT)

    private fun show(state: FolderRulesUiState, onEvent: (FolderRulesEvent) -> Unit = {}, onBack: () -> Unit = {}) =
        composeRule.setContent { FolderRulesScreen(state, onEvent, onBack) }

    @Test
    fun loading_showsLoadingIndicator() {
        show(FolderRulesUiState.Loading)

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun content_showsExplanation() {
        show(FolderRulesUiState.Content(emptyList(), ""))

        composeRule
            .onNodeWithText(
                "Default shows a folder if it has images. Include always shows it; Exclude always hides it.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun row_showsNamePathAndCount() {
        show(FolderRulesUiState.Content(listOf(row), ""))

        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("DCIM/Camera · 1,204 images").assertIsDisplayed()
    }

    @Test
    fun row_withoutImages_showsOnlyThePath() {
        show(FolderRulesUiState.Content(listOf(row.copy(itemCount = 0)), ""))

        composeRule.onNodeWithText("DCIM/Camera").assertIsDisplayed()
    }

    @Test
    fun row_marksTheCurrentChoice() {
        show(FolderRulesUiState.Content(listOf(row.copy(choice = RuleChoice.EXCLUDE)), ""))

        composeRule.onNodeWithText("Exclude").assertIsSelected()
    }

    @Test
    fun choiceClicked_reportsFolderAndChoice() {
        var event: FolderRulesEvent? = null
        show(FolderRulesUiState.Content(listOf(row), ""), onEvent = { event = it })

        composeRule.onNodeWithText("Include").performClick()

        assertEquals(FolderRulesEvent.ChoiceChanged("DCIM/Camera", RuleChoice.INCLUDE), event)
    }

    @Test
    fun toggling_updatesTheSelectedSegment() {
        composeRule.setContent {
            var choice by remember { mutableStateOf(RuleChoice.DEFAULT) }
            FolderRulesScreen(
                state = FolderRulesUiState.Content(listOf(row.copy(choice = choice)), ""),
                onEvent = { if (it is FolderRulesEvent.ChoiceChanged) choice = it.choice },
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Include").performClick()

        composeRule.onNodeWithText("Include").assertIsSelected()
    }

    @Test
    fun patternField_showsExactWording() {
        show(FolderRulesUiState.Content(emptyList(), ""))

        composeRule.onNodeWithText("Also exclude folders matching").assertIsDisplayed()
        composeRule.onNodeWithText("* matches any characters. One pattern per line.").assertIsDisplayed()
    }

    @Test
    fun patternTyped_reportsText() {
        var event: FolderRulesEvent? = null
        show(FolderRulesUiState.Content(emptyList(), ""), onEvent = { event = it })

        composeRule.onNodeWithText("Also exclude folders matching").performTextInput("Old*")

        assertEquals(FolderRulesEvent.PatternsChanged("Old*"), event)
    }

    @Test
    fun backClicked_goesBack() {
        var clicks = 0
        show(FolderRulesUiState.Loading, onBack = { clicks++ })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, clicks)
    }
}
