package net.drehtuer.nextgallery.feature.remote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import net.drehtuer.nextgallery.core.common.AppError
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteBrowserEffectsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val effects = Channel<RemoteBrowserEffect>(Channel.BUFFERED)
    private val events = mutableListOf<RemoteBrowserEvent>()
    private var opened: String? = null

    @Before
    fun setUp() {
        composeRule.setContent {
            RemoteBrowserWithEffects(
                state = RemoteBrowserUiState(isLoading = false),
                effects = effects.receiveAsFlow(),
                onEvent = { events += it },
                onOpenViewer = { opened = it },
                onSignIn = {},
            )
        }
    }

    private fun send(effect: RemoteBrowserEffect) {
        effects.trySend(effect)
        composeRule.waitForIdle()
    }

    @Test
    fun openViewer_opensThePath() {
        send(RemoteBrowserEffect.OpenViewer("/a.jpg"))

        assertEquals("/a.jpg", opened)
    }

    @Test
    fun moved_showsTheMessageWithTheFolderName() {
        send(RemoteBrowserEffect.Moved(3, "/Photos/Italy"))

        composeRule.onNodeWithText("3 images moved to Italy").assertIsDisplayed()
    }

    @Test
    fun moved_undoMovesBack() {
        send(RemoteBrowserEffect.Moved(1, "/Italy"))

        composeRule.onNodeWithText("Undo").performClick()

        composeRule.runOnIdle { assertEquals(listOf<RemoteBrowserEvent>(MoveEvent.Undo), events) }
    }

    @Test
    fun deleted_showsTheMessageOfTheDesign() {
        send(RemoteBrowserEffect.Deleted(1))

        composeRule.onNodeWithText("1 image moved to the Nextcloud trash bin").assertIsDisplayed()
    }

    @Test
    fun deleted_undoRestores() {
        send(RemoteBrowserEffect.Deleted(1))

        composeRule.onNodeWithText("Undo").performClick()

        composeRule.runOnIdle { assertEquals(listOf<RemoteBrowserEvent>(ActionEvent.UndoDelete), events) }
    }

    @Test
    fun failed_showsTheMessage() {
        send(RemoteBrowserEffect.Failed(AppError.Forbidden))

        composeRule.onNodeWithText("Your account isn't allowed to do this.").assertIsDisplayed()
    }

    @Test
    fun offlineMove_showsTheMessageOfTheDesign() {
        send(RemoteBrowserEffect.Offline(RemoteOfflineAction.MOVE))

        composeRule.onNodeWithText("You're offline. Moving needs a connection to your server.").assertIsDisplayed()
    }

    @Test
    fun offlineShare_showsTheMessageOfTheDesign() {
        send(RemoteBrowserEffect.Offline(RemoteOfflineAction.SHARE))

        composeRule.onNodeWithText("You're offline. Sharing needs a connection to your server.").assertIsDisplayed()
    }

    @Test
    fun offlineDelete_explains() {
        send(RemoteBrowserEffect.Offline(RemoteOfflineAction.DELETE))

        composeRule.onNodeWithText("You're offline. Deleting needs a connection to your server.").assertIsDisplayed()
    }
}
