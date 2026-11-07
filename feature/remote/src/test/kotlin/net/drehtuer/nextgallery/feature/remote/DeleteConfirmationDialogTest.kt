package net.drehtuer.nextgallery.feature.remote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.RemoteItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteConfirmationDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RemoteBrowserEvent>()

    private fun show(vararg items: RemoteItem) = composeRule.setContent {
        DeleteConfirmationDialog(DeleteConfirmationState(items.toList()), { events += it })
    }

    @Test
    fun oneImage_saysItCannotBeRestored() {
        show(IMAGE_A)

        composeRule.onNodeWithText(
            "The trash bin on your Nextcloud is off or couldn't be checked, so this image can't be restored.",
        ).assertIsDisplayed()
    }

    @Test
    fun severalImages_namesTheirNumber() {
        show(IMAGE_A, IMAGE_B)

        composeRule.onNodeWithText(
            "The trash bin on your Nextcloud is off or couldn't be checked, so these 2 images can't be restored.",
        ).assertIsDisplayed()
    }

    @Test
    fun delete_sendsTheConfirmation() {
        show(IMAGE_A)

        composeRule.onNodeWithText("Delete").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(ActionEvent.ConfirmDelete), events)
    }

    @Test
    fun cancel_sendsTheCancellation() {
        show(IMAGE_A)

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf<RemoteBrowserEvent>(ActionEvent.CancelDelete), events)
    }
}
