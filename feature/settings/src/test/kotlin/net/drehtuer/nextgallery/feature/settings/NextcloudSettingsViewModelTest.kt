package net.drehtuer.nextgallery.feature.settings

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.nextcloud.ACCESS_LOCAL_NETWORK_PERMISSION
import net.drehtuer.nextgallery.core.data.nextcloud.LocalNetworkPermissionSource
import net.drehtuer.nextgallery.core.data.nextcloud.LoginState
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFileOperations
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFolderContent
import net.drehtuer.nextgallery.core.data.nextcloud.TrustedCertificateRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NextcloudSettingsViewModelTest {
    private val account = MutableStateFlow<NextcloudAccount?>(null)
    private val login = MutableSharedFlow<LoginState>()
    private val accounts = mockk<NextcloudAccountRepository>(relaxed = true) {
        every { observeAccount() } returns account
        every { login(any()) } returns login
    }
    private val certificates = mockk<TrustedCertificateRepository>(relaxed = true) {
        coEvery { accept(any()) } returns AppResult.Success(Unit)
    }
    private val localNetwork = mockk<LocalNetworkPermissionSource> { every { isGranted() } returns true }
    private val settings = mockk<SettingsRepository>(relaxed = true) {
        every { observeSettings() } returns flowOf(UserSettings(remoteRoot = "/Photos"))
    }
    private val operations = mockk<RemoteFileOperations>(relaxed = true)
    private val info = CertificateInfo("cloud.example.com", "CN=a", "CN=b", 1, 2, "AA:BB")
    private val signedIn = NextcloudAccount("https://cloud.example.com", "anna", "id", "pw")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(signInAgain: Boolean = false) =
        NextcloudSettingsViewModel(signInAgain, accounts, certificates, localNetwork, settings, operations)

    /** Keeps the state flow subscribed for the test. */
    private fun TestScope.subscribed(viewModel: NextcloudSettingsViewModel) = viewModel.also {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.state.collect {} }
    }

    private fun NextcloudSettingsViewModel.enterAddress() =
        onEvent(NextcloudSettingsEvent.ServerAddressChanged("cloud.example.com"))

    @Test
    fun state_noAccount_showsTheForm() = runTest {
        val viewModel = subscribed(viewModel())

        assertTrue(viewModel.state.value.showForm)
        assertNull(viewModel.state.value.accountLabel)
    }

    @Test
    fun state_withAccount_showsTheAccountAndRoot() = runTest {
        account.value = signedIn

        val state = subscribed(viewModel()).state.value

        assertFalse(state.showForm)
        assertEquals("anna@cloud.example.com", state.accountLabel)
        assertEquals("/Photos", state.remoteRoot)
    }

    @Test
    fun state_signInAgain_showsTheFormWithTheServer() = runTest {
        account.value = signedIn

        val state = subscribed(viewModel(signInAgain = true)).state.value

        assertTrue(state.showForm)
        assertEquals("https://cloud.example.com", state.serverAddress)
    }

    @Test
    fun state_signInAgain_fieldCanBeCleared() = runTest {
        account.value = signedIn
        val viewModel = subscribed(viewModel(signInAgain = true))

        viewModel.onEvent(NextcloudSettingsEvent.ServerAddressChanged(""))

        assertEquals("", viewModel.state.value.serverAddress)
    }

    @Test
    fun onEvent_serverAddressChanged_updatesAndClearsTheError() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.enterAddress()
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)
        login.emit(LoginState.Failed(AppError.InvalidServerUrl))

        viewModel.onEvent(NextcloudSettingsEvent.ServerAddressChanged("x"))

        assertEquals("x", viewModel.state.value.serverAddress)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun onEvent_signIn_permissionMissing_showsTheExplanationFirst() = runTest {
        every { localNetwork.isGranted() } returns false
        val viewModel = subscribed(viewModel())
        viewModel.enterAddress()

        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        assertTrue(viewModel.state.value.showLocalNetworkExplanation)
        io.mockk.verify(exactly = 0) { accounts.login(any()) }
    }

    @Test
    fun onEvent_explanationAccepted_requestsThePermission() = runTest {
        every { localNetwork.isGranted() } returns false
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        viewModel.effects.test {
            viewModel.onEvent(NextcloudSettingsEvent.LocalNetworkExplanationAccepted)

            assertEquals(NextcloudSettingsEffect.RequestPermission(ACCESS_LOCAL_NETWORK_PERMISSION), awaitItem())
        }
        assertFalse(viewModel.state.value.showLocalNetworkExplanation)
    }

    @Test
    fun onEvent_explanationDismissed_doesNotSignIn() = runTest {
        every { localNetwork.isGranted() } returns false
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        viewModel.onEvent(NextcloudSettingsEvent.LocalNetworkExplanationDismissed)

        assertFalse(viewModel.state.value.showLocalNetworkExplanation)
        io.mockk.verify(exactly = 0) { accounts.login(any()) }
    }

    @Test
    fun onEvent_permissionRequestFinished_startsTheLoginAnyway() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.enterAddress()

        viewModel.onEvent(NextcloudSettingsEvent.LocalNetworkRequestFinished)

        io.mockk.verify { accounts.login("cloud.example.com") }
    }

    @Test
    fun onEvent_signIn_permissionGranted_startsTheLogin() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.enterAddress()

        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        assertTrue(viewModel.state.value.isSigningIn)
        io.mockk.verify { accounts.login("cloud.example.com") }
    }

    @Test
    fun login_started_opensTheBrowser() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.enterAddress()
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        viewModel.effects.test {
            login.emit(LoginState.Started("https://cloud.example.com/login"))

            assertEquals(NextcloudSettingsEffect.OpenBrowser("https://cloud.example.com/login"), awaitItem())
        }
    }

    @Test
    fun login_polling_keepsWaiting() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        login.emit(LoginState.Polling)

        assertTrue(viewModel.state.value.isSigningIn)
    }

    @Test
    fun login_success_reportsSignedInAndEndsWaiting() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        viewModel.effects.test {
            login.emit(LoginState.Success(signedIn))

            assertEquals(NextcloudSettingsEffect.SignedIn, awaitItem())
        }
        assertFalse(viewModel.state.value.isSigningIn)
    }

    @Test
    fun login_failed_showsTheError() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        login.emit(LoginState.Failed(AppError.LoginExpired))

        assertEquals(AppError.LoginExpired, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSigningIn)
    }

    @Test
    fun onEvent_cancelSignIn_endsWaiting() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        viewModel.onEvent(NextcloudSettingsEvent.CancelSignIn)

        assertFalse(viewModel.state.value.isSigningIn)
        assertEquals(0, login.subscriptionCount.value)
    }

    @Test
    fun login_untrustedCertificate_showsTheDialogAndPinsNothing() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)

        login.emit(LoginState.Failed(AppError.UntrustedCertificate(info)))

        assertEquals(info, viewModel.state.value.certificate)
        coVerify(exactly = 0) { certificates.accept(any()) }
    }

    @Test
    fun onEvent_trustCertificate_pinsAndRetriesTheLogin() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.enterAddress()
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)
        login.emit(LoginState.Failed(AppError.UntrustedCertificate(info)))

        viewModel.onEvent(NextcloudSettingsEvent.TrustCertificate)

        coVerify { certificates.accept(info) }
        io.mockk.verify(exactly = 2) { accounts.login("cloud.example.com") }
    }

    @Test
    fun onEvent_trustCertificateFails_showsTheErrorWithoutRetry() = runTest {
        coEvery { certificates.accept(any()) } returns AppResult.Failure(AppError.Io("disk"))
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)
        login.emit(LoginState.Failed(AppError.UntrustedCertificate(info)))

        viewModel.onEvent(NextcloudSettingsEvent.TrustCertificate)

        assertEquals(AppError.Io("disk"), viewModel.state.value.error)
        io.mockk.verify(exactly = 1) { accounts.login(any()) }
    }

    @Test
    fun onEvent_rejectCertificate_showsAnErrorAndPinsNothing() = runTest {
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.SignIn)
        login.emit(LoginState.Failed(AppError.UntrustedCertificate(info)))

        viewModel.onEvent(NextcloudSettingsEvent.RejectCertificate)

        assertNull(viewModel.state.value.certificate)
        assertEquals(AppError.UntrustedCertificate(info), viewModel.state.value.error)
        coVerify(exactly = 0) { certificates.accept(any()) }
    }

    @Test
    fun onEvent_trustWithoutACertificate_doesNothing() = runTest {
        subscribed(viewModel()).onEvent(NextcloudSettingsEvent.TrustCertificate)

        coVerify(exactly = 0) { certificates.accept(any()) }
    }

    @Test
    fun onEvent_logOut_logsOut() = runTest {
        subscribed(viewModel()).onEvent(NextcloudSettingsEvent.LogOut)

        coVerify { accounts.logout() }
    }

    @Test
    fun onEvent_openRootPicker_listsTheCurrentRoot() = runTest {
        val folder = RemoteItem("/Photos/a", "a", true)
        coEvery { operations.listFolder("/Photos") } returns
            AppResult.Success(RemoteFolderContent(null, listOf(folder)))
        val viewModel = subscribed(viewModel())

        viewModel.onEvent(NextcloudSettingsEvent.OpenRootPicker)

        assertEquals(
            RemoteRootPickerState("/Photos", listOf(folder), isLoading = false),
            viewModel.state.value.rootPicker,
        )
    }

    @Test
    fun onEvent_rootPickerOpened_listsThatFolder() = runTest {
        coEvery { operations.listFolder("/x") } returns AppResult.Success(RemoteFolderContent(null, emptyList()))
        val viewModel = subscribed(viewModel())

        viewModel.onEvent(NextcloudSettingsEvent.RootPickerOpened("/x"))

        assertEquals("/x", viewModel.state.value.rootPicker?.path)
    }

    @Test
    fun onEvent_rootPickerListFails_showsTheError() = runTest {
        coEvery { operations.listFolder("/x") } returns AppResult.Failure(AppError.NotFound)
        val viewModel = subscribed(viewModel())

        viewModel.onEvent(NextcloudSettingsEvent.RootPickerOpened("/x"))

        assertEquals(AppError.NotFound, viewModel.state.value.rootPicker?.error)
    }

    @Test
    fun onEvent_rootPickerConfirmed_storesThePathAndCloses() = runTest {
        coEvery { operations.listFolder("/x") } returns AppResult.Success(RemoteFolderContent(null, emptyList()))
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.RootPickerOpened("/x"))

        viewModel.onEvent(NextcloudSettingsEvent.RootPickerConfirmed)

        coVerify { settings.setRemoteRoot("/x") }
        assertNull(viewModel.state.value.rootPicker)
    }

    @Test
    fun onEvent_rootPickerConfirmedWithoutPicker_storesNothing() = runTest {
        subscribed(viewModel()).onEvent(NextcloudSettingsEvent.RootPickerConfirmed)

        coVerify(exactly = 0) { settings.setRemoteRoot(any()) }
    }

    @Test
    fun onEvent_rootPickerDismissed_closes() = runTest {
        coEvery { operations.listFolder("/x") } returns AppResult.Success(RemoteFolderContent(null, emptyList()))
        val viewModel = subscribed(viewModel())
        viewModel.onEvent(NextcloudSettingsEvent.RootPickerOpened("/x"))

        viewModel.onEvent(NextcloudSettingsEvent.RootPickerDismissed)

        assertNull(viewModel.state.value.rootPicker)
    }
}
