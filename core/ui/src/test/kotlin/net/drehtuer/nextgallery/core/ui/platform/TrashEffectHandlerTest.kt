package net.drehtuer.nextgallery.core.ui.platform

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import net.drehtuer.nextgallery.core.model.TrashEffect
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashEffectHandlerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val confirmationResults = mutableListOf<Boolean>()
    private var undos = 0
    private lateinit var handle: (TrashEffect) -> Unit

    private fun show(resultCode: Int = Activity.RESULT_OK) {
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                dispatchResult(requestCode, ActivityResult(resultCode, null))
            }
        }
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                val snackbar = remember { SnackbarHostState() }
                handle = rememberTrashEffectHandler(snackbar, { confirmationResults += it }, { undos++ })
                SnackbarHost(snackbar)
            }
        }
    }

    private fun confirmation(): PlatformConfirmation {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        return PlatformConfirmation(intent.intentSender)
    }

    @Test
    fun requestConfirmation_accepted_reportsConfirmed() {
        show(Activity.RESULT_OK)

        composeRule.runOnIdle { handle(TrashEffect.RequestConfirmation(confirmation())) }
        composeRule.waitForIdle()

        assertEquals(listOf(true), confirmationResults)
    }

    @Test
    fun requestConfirmation_declined_reportsNotConfirmed() {
        show(Activity.RESULT_CANCELED)

        composeRule.runOnIdle { handle(TrashEffect.RequestConfirmation(confirmation())) }
        composeRule.waitForIdle()

        assertEquals(listOf(false), confirmationResults)
    }

    @Test
    fun movedToTrash_showsCount() {
        show()

        composeRule.runOnIdle { handle(TrashEffect.MovedToTrash(2)) }

        composeRule.onNodeWithText("2 images moved to the trash").assertIsDisplayed()
    }

    @Test
    fun movedToTrash_undoClicked_invokesCallback() {
        show()
        composeRule.runOnIdle { handle(TrashEffect.MovedToTrash(1)) }

        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()

        assertEquals(1, undos)
    }

    @Test
    fun failed_showsMessage() {
        show()

        composeRule.runOnIdle { handle(TrashEffect.Failed) }

        composeRule.onNodeWithText("The images could not be moved to the trash").assertIsDisplayed()
    }
}
