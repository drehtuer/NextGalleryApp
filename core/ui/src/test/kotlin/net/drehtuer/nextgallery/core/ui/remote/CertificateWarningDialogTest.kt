package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.CertificateInfo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CertificateWarningDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val info = CertificateInfo("cloud.example.com", "CN=cloud", "CN=my ca", 0, 4_102_444_800_000, "AA:BB")
    private val changed = info.copy(pinnedFingerprint = "CC:DD")
    private var result: String? = null

    private fun show(certificate: CertificateInfo) = composeRule.setContent {
        CertificateWarningDialog(certificate, onTrust = { result = "trust" }, onReject = { result = "reject" })
    }

    @Test
    fun unknown_showsTheDetails() {
        show(info)

        composeRule.onNodeWithText("Can't verify this server").assertIsDisplayed()
        composeRule.onNodeWithText("CN=cloud").assertIsDisplayed()
        composeRule.onNodeWithText("CN=my ca").assertIsDisplayed()
        composeRule.onNodeWithText("AA:BB").assertIsDisplayed()
        composeRule.onNodeWithText("Valid until").assertIsDisplayed()
    }

    @Test
    fun unknown_trustClicked_trusts() {
        show(info)

        composeRule.onNodeWithText("Trust this certificate").performClick()

        assertEquals("trust", result)
    }

    @Test
    fun unknown_cancelClicked_rejects() {
        show(info)

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals("reject", result)
    }

    @Test
    fun changed_showsBothFingerprintsAndTheHost() {
        show(changed)

        composeRule.onNodeWithText("The server's certificate changed").assertIsDisplayed()
        composeRule.onNodeWithText("cloud.example.com now shows", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("CC:DD").assertIsDisplayed()
        composeRule.onNodeWithText("AA:BB").assertIsDisplayed()
    }

    @Test
    fun changed_trustClicked_trusts() {
        show(changed)

        composeRule.onNodeWithText("Trust the new one").performClick()

        assertEquals("trust", result)
    }

    @Test
    fun changed_disconnectClicked_rejects() {
        show(changed)

        composeRule.onNodeWithText("Disconnect").performClick()

        assertEquals("reject", result)
    }
}
