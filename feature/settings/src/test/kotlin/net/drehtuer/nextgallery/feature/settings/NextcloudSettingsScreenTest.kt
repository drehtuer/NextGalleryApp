package net.drehtuer.nextgallery.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.model.RemoteItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NextcloudSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<NextcloudSettingsEvent>()
    private var openedCertificates = false

    private fun show(state: NextcloudSettingsUiState) = composeRule.setContent {
        NextcloudSettingsScreen(state, {
            events += it
        }, onBack = {}, onOpenCertificates = { openedCertificates = true })
    }

    @Test
    fun signedOut_showsTheWordingOfTheDesign() {
        show(NextcloudSettingsUiState())

        composeRule.onNodeWithText("Connect your Nextcloud").assertIsDisplayed()
        composeRule.onNodeWithText("Browse the photos on your own server.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Server address").assertIsDisplayed()
    }

    @Test
    fun signedOut_noAddress_signInIsDisabled() {
        show(NextcloudSettingsUiState())

        composeRule.onNodeWithText("Sign in").assertIsNotEnabled()
    }

    @Test
    fun signedOut_withAddress_signInSendsTheEvent() {
        show(NextcloudSettingsUiState(serverAddress = "cloud.example.com"))

        composeRule.onNodeWithText("Sign in").assertIsEnabled().performClick()

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.SignIn), events)
    }

    @Test
    fun typing_sendsTheAddress() {
        show(NextcloudSettingsUiState())

        composeRule.onNode(hasSetTextAction()).performTextInput("c")

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.ServerAddressChanged("c")), events)
    }

    @Test
    fun signingIn_showsWaitingAndCancel() {
        show(NextcloudSettingsUiState(serverAddress = "cloud.example.com", isSigningIn = true))

        composeRule.onNodeWithText("Waiting for you to sign in in the browser…").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.CancelSignIn), events)
    }

    @Test
    fun error_isShownAtTheField() {
        show(NextcloudSettingsUiState(serverAddress = "x", error = AppError.LocalNetworkBlocked))

        composeRule.onNodeWithText("local network", substring = true).assertIsDisplayed()
    }

    @Test
    fun signedIn_showsTheAccountAndRoot() {
        show(NextcloudSettingsUiState(accountLabel = "anna@cloud.example.com", remoteRoot = "/Photos"))

        composeRule.onNodeWithText("anna@cloud.example.com").assertIsDisplayed()
        composeRule.onNodeWithText("/Photos").assertIsDisplayed()
    }

    @Test
    fun signedIn_logOutSendsTheEvent() {
        show(NextcloudSettingsUiState(accountLabel = "anna@cloud.example.com"))

        composeRule.onNodeWithText("Log out").performScrollTo().performClick()

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.LogOut), events)
    }

    @Test
    fun signedIn_rootRowOpensThePicker() {
        show(NextcloudSettingsUiState(accountLabel = "anna@cloud.example.com", remoteRoot = "/Photos"))

        composeRule.onNodeWithText("/Photos").performClick()

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.OpenRootPicker), events)
    }

    @Test
    fun certificatesRow_opensTheList() {
        show(NextcloudSettingsUiState())

        composeRule.onNodeWithText("Accepted certificates").performScrollTo().performClick()

        assertTrue(openedCertificates)
    }

    @Test
    fun certificate_showsTheDialogAndSendsTheDecision() {
        val info = CertificateInfo("cloud.example.com", "CN=a", "CN=b", 1, 2, "AA:BB")
        show(NextcloudSettingsUiState(certificate = info))

        composeRule.onNodeWithText("Trust this certificate").performClick()

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.TrustCertificate), events)
    }

    @Test
    fun localNetworkExplanation_continueSendsTheEvent() {
        show(NextcloudSettingsUiState(showLocalNetworkExplanation = true))

        composeRule.onNodeWithText("Continue").performClick()

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.LocalNetworkExplanationAccepted), events)
    }

    @Test
    fun rootPickerContent_folderClickOpensIt() {
        val picker = RemoteRootPickerState("/", listOf(RemoteItem("/a", "a", true)), isLoading = false)
        composeRule.setContent { RemoteRootPickerContent(picker, { events += it }) }

        composeRule.onNodeWithText("a").performClick()

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.RootPickerOpened("/a")), events)
    }

    @Test
    fun rootPickerContent_useSendsConfirm() {
        val picker = RemoteRootPickerState("/Photos", isLoading = false)
        composeRule.setContent { RemoteRootPickerContent(picker, { events += it }) }

        composeRule.onNodeWithText("Use “Photos”").performClick()

        assertEquals(listOf<NextcloudSettingsEvent>(NextcloudSettingsEvent.RootPickerConfirmed), events)
    }

    @Test
    fun rootPickerContent_error_isShown() {
        val picker = RemoteRootPickerState("/", isLoading = false, error = AppError.Network(null))
        composeRule.setContent { RemoteRootPickerContent(picker, { events += it }) }

        composeRule.onNodeWithText("Can't reach your server", substring = true).assertIsDisplayed()
    }
}
