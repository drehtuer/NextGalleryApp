package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollbarSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var scrolledTo: Float? = null

    private fun show(positionText: String? = null) = composeRule.setContent {
        Box(
            Modifier
                .size(48.dp)
                .testTag("thumb")
                .scrollbarSemantics("Position", { 0.5f }, { positionText }, { scrolledTo = it }),
        )
    }

    @Test
    fun noPositionText_leavesThePercentageToTheScreenReader() {
        show()

        composeRule.onNodeWithTag("thumb")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun setProgress_belowTheRange_isTheStart() {
        show()

        composeRule.onNodeWithTag("thumb").performSemanticsAction(SemanticsActions.SetProgress) { it(-1f) }

        assertEquals(0f, scrolledTo)
    }

    @Test
    fun setProgress_aboveTheRange_isTheEnd() {
        show()

        composeRule.onNodeWithTag("thumb").performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }

        assertEquals(1f, scrolledTo)
    }

    @Test
    fun setProgress_reportsItsTarget() {
        show()

        composeRule.onNodeWithTag("thumb").performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }

        assertEquals(0.25f, scrolledTo)
    }
}
