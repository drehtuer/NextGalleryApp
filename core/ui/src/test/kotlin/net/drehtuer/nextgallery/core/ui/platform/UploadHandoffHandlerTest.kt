package net.drehtuer.nextgallery.core.ui.platform

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.UploadHandoff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadHandoffHandlerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val started = mutableListOf<Intent>()
    private var resumes = 0
    private lateinit var handle: (UploadHandoff) -> Unit
    private val ready = UploadHandoff.Ready(
        ShareSpec(listOf("content://media/1"), "image/jpeg", "com.nextcloud.client"),
    )

    private fun show(startFailure: RuntimeException? = null) {
        composeRule.setContent {
            val base = LocalContext.current
            val context = remember(base) {
                object : ContextWrapper(base) {
                    override fun startActivity(intent: Intent) {
                        startFailure?.let { throw it }
                        started += intent
                    }
                }
            }
            CompositionLocalProvider(LocalContext provides context) {
                val snackbar = remember { SnackbarHostState() }
                handle = rememberUploadHandoffHandler(snackbar) { resumes++ }
                SnackbarHost(snackbar)
            }
        }
    }

    @Test
    fun ready_opensTheNextcloudApp() {
        show()

        composeRule.runOnIdle { handle(ready) }
        composeRule.waitForIdle()

        assertEquals(listOf("com.nextcloud.client"), started.map { it.`package` })
    }

    @Test
    fun ready_appGoneMeanwhile_showsTheInstallHint() {
        show(ActivityNotFoundException())

        composeRule.runOnIdle { handle(ready) }

        composeRule.onNodeWithText("Install the Nextcloud app to upload photos.").assertIsDisplayed()
    }

    @Test
    fun ready_systemRefuses_showsAMessage() {
        show(IllegalStateException("too many"))

        composeRule.runOnIdle { handle(ready) }

        composeRule.onNodeWithText("Couldn't pass these photos to the Nextcloud app. Try fewer at a time.")
            .assertIsDisplayed()
    }

    @Test
    fun nextcloudAppMissing_showsTheInstallHint() {
        show()

        composeRule.runOnIdle { handle(UploadHandoff.NextcloudAppMissing) }

        composeRule.onNodeWithText("Install the Nextcloud app to upload photos.").assertIsDisplayed()
    }

    @Test
    fun nothingToUpload_showsAMessage() {
        show()

        composeRule.runOnIdle { handle(UploadHandoff.NothingToUpload) }

        composeRule.onNodeWithText("Already on your Nextcloud.").assertIsDisplayed()
        assertTrue(started.isEmpty())
    }

    @Test
    fun screenResumed_reportsIt() {
        show()

        composeRule.waitForIdle()

        assertEquals(1, resumes)
    }
}
