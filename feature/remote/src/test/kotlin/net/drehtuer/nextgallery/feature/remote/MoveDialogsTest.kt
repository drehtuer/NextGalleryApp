package net.drehtuer.nextgallery.feature.remote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.data.nextcloud.ConflictPolicy
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoveDialogsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RemoteBrowserEvent>()
    private val existing = IMAGE_A.copy(path = "/P/Italy/a.jpg", size = 1024)

    private fun showConflict(otherImages: Int = 0) = composeRule.setContent {
        MoveConflictDialog(MoveConflictState(IMAGE_A, existing, "/P/Italy", otherImages), { events += it })
    }

    @Test
    fun conflict_namesTheFileAndFolder() {
        showConflict()

        composeRule.onNodeWithText("a.jpg is already in Italy").assertIsDisplayed()
    }

    @Test
    fun conflict_showsSizesOfBothFiles() {
        showConflict()

        composeRule.onNodeWithText("This image:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("In Italy:", substring = true).assertIsDisplayed()
    }

    @Test
    fun conflict_keepBothSendsThePolicy() {
        showConflict()

        composeRule.onNodeWithText("Keep both").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.ConflictResolved(ConflictPolicy.KEEP_BOTH, false)), events)
    }

    @Test
    fun conflict_replaceSendsThePolicy() {
        showConflict()

        composeRule.onNodeWithText("Replace the one in Italy").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.ConflictResolved(ConflictPolicy.REPLACE, false)), events)
    }

    @Test
    fun conflict_skipSendsThePolicy() {
        showConflict()

        composeRule.onNodeWithText("Skip this image").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.ConflictResolved(ConflictPolicy.SKIP, false)), events)
    }

    @Test
    fun conflict_lastImage_hasNoCheckbox() {
        showConflict(otherImages = 0)

        composeRule.onNodeWithText("Do the same", substring = true).assertDoesNotExist()
    }

    @Test
    fun conflict_doTheSameIsSentWithTheAnswer() {
        showConflict(otherImages = 2)

        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNodeWithText("Keep both").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.ConflictResolved(ConflictPolicy.KEEP_BOTH, true)), events)
    }

    @Test
    fun conflict_oneOtherImage_usesTheSingularWording() {
        showConflict(otherImages = 1)

        composeRule.onNodeWithText("Do the same for the other conflict").assertIsDisplayed()
    }

    @Test
    fun newFolder_emptyName_cannotBeCreated() {
        composeRule.setContent { NewFolderDialog { events += it } }

        composeRule.onNodeWithText("Create").assertIsNotEnabled()
    }

    @Test
    fun newFolder_nameWithSlash_cannotBeCreated() {
        composeRule.setContent { NewFolderDialog { events += it } }

        composeRule.onNode(hasSetTextAction()).performTextInput("a/b")

        composeRule.onNodeWithText("Create").assertIsNotEnabled()
    }

    @Test
    fun newFolder_createSendsTheName() {
        composeRule.setContent { NewFolderDialog { events += it } }

        composeRule.onNode(hasSetTextAction()).performTextInput("Holiday")
        composeRule.onNodeWithText("Create").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.NewFolderConfirmed("Holiday")), events)
    }

    @Test
    fun newFolder_cancelSendsTheEvent() {
        composeRule.setContent { NewFolderDialog { events += it } }

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.NewFolderDismissed), events)
    }
}
