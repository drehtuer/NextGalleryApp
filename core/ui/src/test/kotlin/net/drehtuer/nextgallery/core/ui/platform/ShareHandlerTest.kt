package net.drehtuer.nextgallery.core.ui.platform

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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareHandlerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val started = mutableListOf<Intent>()
    private lateinit var handle: (ShareSpec) -> Unit
    private val spec = ShareSpec(listOf("content://media/1"), "image/jpeg")

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
                handle = rememberShareHandler(snackbar)
                SnackbarHost(snackbar)
            }
        }
    }

    @Test
    fun share_opensTheChooser() {
        show()

        composeRule.runOnIdle { handle(spec) }

        assertEquals(listOf(Intent.ACTION_CHOOSER), started.map { it.action })
    }

    @Test
    fun share_systemRefusesTooManyImages_showsAMessageInsteadOfCrashing() {
        show(RuntimeException("Failure from system"))

        composeRule.runOnIdle { handle(spec) }

        composeRule.onNodeWithText("Couldn't share this many photos at once. Try fewer.").assertIsDisplayed()
    }

    @Test
    fun share_systemAccepts_showsNoMessage() {
        show()

        composeRule.runOnIdle { handle(spec) }

        composeRule.onNodeWithText("Couldn't share this many photos at once. Try fewer.").assertDoesNotExist()
    }
}
