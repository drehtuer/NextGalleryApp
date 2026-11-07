package net.drehtuer.nextgallery.core.data.nextcloud

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.data.fakes.FakeClock
import net.drehtuer.nextgallery.core.data.upload.UploadStateStore
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.nextcloud.ConnectionEvictor
import net.drehtuer.nextgallery.core.nextcloud.LoginExpiredException
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowResult
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowSession
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowV2Client
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpException
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import net.drehtuer.nextgallery.core.nextcloud.ServerUrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultNextcloudAccountRepositoryTest {
    private val loginFlow = mockk<LoginFlowV2Client>()
    private val ocs = mockk<OcsClient>(relaxed = true)
    private val store = FakeAccountStore(null)
    private val cache = mockk<RemoteCacheStore>(relaxed = true)
    private val images = mockk<RemoteImageCache>(relaxed = true)
    private val shares = mockk<RemoteShareCache>(relaxed = true)
    private val connections = mockk<ConnectionEvictor>(relaxed = true)
    private val uploadStates = mockk<UploadStateStore>(relaxed = true)
    private val policy = LocalNetworkPolicy(GrantedLocalNetwork) { emptyList() }

    private val clock = FakeClock(now = START_MILLIS)
    private val inForeground = flowOf(true)
    private val session = LoginFlowSession("https://cloud.example.com/login", "https://cloud.example.com/poll", "token")
    private val flowResult = LoginFlowResult("https://cloud.example.com", "anna", "app-password")

    private fun repository(networkPolicy: LocalNetworkPolicy = policy) = DefaultNextcloudAccountRepository(
        normalizer = ServerUrlNormalizer(allowCleartext = false),
        loginFlow = loginFlow,
        ocs = ocs,
        accounts = store,
        session = AccountSession(
            store,
            RemoteDataWiper(cache, images, shares, connections, uploadStates),
            AppPasswordRevoker(ocs, CoroutineScope(Dispatchers.Unconfined)),
        ),
        localNetwork = networkPolicy,
        clock = clock,
    )

    private val pending = PendingLogin(
        serverUrl = "https://cloud.example.com",
        pollEndpoint = session.pollEndpoint,
        token = session.token,
        startedAtMillis = START_MILLIS,
    )

    private fun loginSucceeds() {
        coEvery { loginFlow.start("https://cloud.example.com") } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns flowResult
        coEvery { ocs.userId("https://cloud.example.com", "anna", "app-password") } returns "anna-id"
    }

    @Test
    fun login_success_emitsStartedPollingSuccess() = runTest {
        loginSucceeds()

        val states = repository().login("cloud.example.com", inForeground).toList()

        assertEquals(
            listOf(
                LoginState.Started(session.loginUrl, pending),
                LoginState.Polling,
                LoginState.Success(TEST_ACCOUNT),
            ),
            states,
        )
    }

    @Test
    fun login_success_storesTheAccount() = runTest {
        loginSucceeds()

        repository().login("cloud.example.com", inForeground).toList()

        assertEquals(TEST_ACCOUNT, store.account.value)
    }

    @Test
    fun login_invalidUrl_failsWithoutNetwork() = runTest {
        val states = repository().login("http://cloud.example.com", inForeground).toList()

        assertEquals(listOf<LoginState>(LoginState.Failed(AppError.InvalidServerUrl)), states)
        coVerify(exactly = 0) { loginFlow.start(any()) }
    }

    @Test
    fun login_startFails_emitsFailed() = runTest {
        coEvery { loginFlow.start(any()) } throws IOException("no route")

        val states = repository().login("cloud.example.com", inForeground).toList()

        assertEquals(listOf<LoginState>(LoginState.Failed(AppError.Network("no route"))), states)
    }

    @Test
    fun login_expired_emitsFailedAndStoresNothing() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } coAnswers {
            clock.now += DefaultNextcloudAccountRepository.LOGIN_TIMEOUT_MILLIS
            null
        }

        val states = repository().login("cloud.example.com", inForeground).toList()

        assertEquals(LoginState.Failed(AppError.LoginExpired), states.last())
        assertNull(store.account.value)
    }

    @Test
    fun login_userIdFails_storesNothing() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns flowResult
        coEvery { ocs.userId(any(), any(), any()) } throws IOException("gone")

        repository().login("cloud.example.com", inForeground).toList()

        assertNull(store.account.value)
    }

    @Test
    fun login_blockedLocalAddress_emitsBlocked() = runTest {
        val denied = object : LocalNetworkPermissionSource {
            override fun isGranted() = false
        }
        val local = LocalNetworkPolicy(denied) { listOf(InetAddress.getByName("192.168.1.5")) }
        coEvery { loginFlow.start(any()) } throws IOException("timeout")

        val states = repository(local).login("192.168.1.5", inForeground).toList()

        assertEquals(listOf<LoginState>(LoginState.Failed(AppError.LocalNetworkBlocked)), states)
    }

    @Test
    fun login_success_doesNotRevokeTheNewPassword() = runTest {
        loginSucceeds()

        repository().login("cloud.example.com", inForeground).toList()

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), "app-password") }
    }

    @Test
    fun login_userIdFails_revokesTheUnusedPassword() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns flowResult
        coEvery { ocs.userId(any(), any(), any()) } throws IOException("gone")

        repository().login("cloud.example.com", inForeground).toList()

        coVerify { ocs.revokeAppPassword("https://cloud.example.com", "anna", "app-password") }
    }

    @Test
    fun login_cancelledWhileFetchingTheUserId_revokesTheUnusedPassword() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns flowResult
        coEvery { ocs.userId(any(), any(), any()) } coAnswers { awaitCancellation() }
        val login =
            launch(UnconfinedTestDispatcher(testScheduler)) {
                repository().login("cloud.example.com", inForeground).collect {}
            }

        login.cancel()
        login.join()

        coVerify { ocs.revokeAppPassword("https://cloud.example.com", "anna", "app-password") }
    }

    @Test
    fun login_userIdFailsAndThePasswordIsAlreadyInUse_doesNotRevokeIt() = runTest {
        store.account.value = TEST_ACCOUNT
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns flowResult
        coEvery { ocs.userId(any(), any(), any()) } throws IOException("gone")

        repository().login("cloud.example.com", inForeground).toList()

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), any()) }
    }

    @Test
    fun login_expired_hasNothingToRevoke() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        clock.now += DefaultNextcloudAccountRepository.LOGIN_TIMEOUT_MILLIS

        repository().resumeLogin(pending, inForeground).toList()

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), any()) }
    }

    @Test
    fun login_responseNamesAnotherHost_failsWithAClearErrorAndStoresNothing() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns
            flowResult.copy(serverUrl = "https://elsewhere.example.com")

        val last = repository().login("cloud.example.com", inForeground).toList().last()

        assertEquals(LoginState.Failed(AppError.ServerMismatch), last)
        assertNull(store.account.value)
    }

    @Test
    fun login_responseNamesAnotherHost_neverSendsThePasswordThere() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns
            flowResult.copy(serverUrl = "https://elsewhere.example.com")

        repository().login("cloud.example.com", inForeground).toList()

        coVerify(exactly = 0) { ocs.userId("https://elsewhere.example.com", any(), any()) }
        coVerify(exactly = 0) { ocs.revokeAppPassword("https://elsewhere.example.com", any(), any()) }
        coVerify { ocs.revokeAppPassword("https://cloud.example.com", "anna", "app-password") }
    }

    @Test
    fun login_responseOnTheSameHostWithAPath_isAccepted() = runTest {
        val withPath = flowResult.copy(serverUrl = "https://cloud.example.com/nextcloud")
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns withPath
        coEvery { ocs.userId("https://cloud.example.com/nextcloud", "anna", "app-password") } returns "anna-id"

        repository().login("cloud.example.com", inForeground).toList()

        assertEquals("https://cloud.example.com/nextcloud", store.account.value?.serverUrl)
    }

    @Test
    fun login_cancelled_storesNothing() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } coAnswers
            { kotlinx.coroutines.awaitCancellation() }

        repository().login("cloud.example.com", inForeground).test {
            awaitItem()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(store.account.value)
    }

    @Test
    fun logout_endsLoggedOut() = runTest {
        store.account.value = TEST_ACCOUNT

        repository().logout()

        assertNull(store.account.value)
    }

    @Test
    fun observeImageUrls_loggedOut_isNull() = runTest {
        assertNull(repository().observeImageUrls().first())
    }

    @Test
    fun observeImageUrls_loggedIn_buildsPreviewAndOriginalUrls() = runTest {
        store.account.value = TEST_ACCOUNT
        val item = RemoteItem("/My Photos/a.jpg", "a.jpg", isDirectory = false, fileId = 42)

        val urls = checkNotNull(repository().observeImageUrls().first())

        assertTrue(urls.preview(item, 256).orEmpty().endsWith("/index.php/core/preview?fileId=42&x=256&y=256&a=1"))
        assertTrue(urls.original(item).endsWith("/remote.php/dav/files/anna-id/My%20Photos/a.jpg"))
    }

    @Test
    fun imageUrls_noFileId_hasNoPreview() = runTest {
        store.account.value = TEST_ACCOUNT
        val item = RemoteItem("/a.jpg", "a.jpg", isDirectory = false)

        val urls = checkNotNull(repository().observeImageUrls().first())

        assertNull(urls.preview(item, 256))
    }

    @Test
    fun imageUrls_serverHasNoPreview_hasNoPreview() = runTest {
        store.account.value = TEST_ACCOUNT
        val item = RemoteItem("/a.jpg", "a.jpg", isDirectory = false, fileId = 1, hasPreview = false)

        val urls = checkNotNull(repository().observeImageUrls().first())

        assertNull(urls.preview(item, 256))
    }

    @Test
    fun imageUrls_toString_hidesThePassword() = runTest {
        store.account.value = TEST_ACCOUNT

        val urls = checkNotNull(repository().observeImageUrls().first())

        assertFalse(urls.toString().contains(TEST_ACCOUNT.appPassword))
    }

    @Test
    fun login_networkFailsWhileWaiting_keepsAskingAndSignsIn() = runTest {
        loginSucceeds()
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } throws IOException("no network") andThen
            flowResult

        val states = repository().login("cloud.example.com", inForeground).toList()

        assertTrue(states.last() is LoginState.Success)
    }

    @Test
    fun login_serverBrieflyUnavailable_keepsAskingAndSignsIn() = runTest {
        loginSucceeds()
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } throws NextcloudHttpException(503) andThen
            flowResult

        val states = repository().login("cloud.example.com", inForeground).toList()

        assertTrue(states.last() is LoginState.Success)
    }

    @Test
    fun login_serverRefusesTheCheck_fails() = runTest {
        loginSucceeds()
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } throws NextcloudHttpException(401)

        val states = repository().login("cloud.example.com", inForeground).toList()

        assertEquals(LoginState.Failed(AppError.Auth), states.last())
    }

    @Test
    fun login_inTheBackground_doesNotAsk() = runTest {
        loginSucceeds()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository().login("cloud.example.com", MutableStateFlow(false)).collect {}
        }

        coVerify(exactly = 0) { loginFlow.check(any(), any()) }
        job.cancel()
    }

    @Test
    fun login_backInTheForeground_asksRightAway() = runTest {
        loginSucceeds()
        val foreground = MutableStateFlow(false)
        val states = mutableListOf<LoginState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository().login("cloud.example.com", foreground).collect { states += it }
        }

        foreground.value = true

        assertTrue(states.last() is LoginState.Success)
        job.cancel()
    }

    @Test
    fun login_notSignedInYet_asksAgainAfterTheInterval() = runTest {
        loginSucceeds()
        coEvery { loginFlow.check(session.pollEndpoint, session.token) } returns null andThen flowResult
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository().login("cloud.example.com", inForeground).collect {}
        }

        testScheduler.advanceTimeBy(DefaultNextcloudAccountRepository.POLL_INTERVAL_MILLIS + 1)

        coVerify(exactly = 2) { loginFlow.check(session.pollEndpoint, session.token) }
        job.cancel()
    }

    @Test
    fun login_started_reportsWhatIsNeededToContinue() = runTest {
        loginSucceeds()

        val started = repository().login("cloud.example.com", inForeground).toList().first() as LoginState.Started

        assertEquals(pending, started.pending)
    }

    @Test
    fun resumeLogin_signedInMeanwhile_storesTheAccount() = runTest {
        loginSucceeds()

        val states = repository().resumeLogin(pending, inForeground).toList()

        assertEquals("anna-id", (states.last() as LoginState.Success).account.userId)
    }

    @Test
    fun resumeLogin_timeUp_failsAsExpired() = runTest {
        clock.now += DefaultNextcloudAccountRepository.LOGIN_TIMEOUT_MILLIS

        val states = repository().resumeLogin(pending, inForeground).toList()

        assertEquals(LoginState.Failed(AppError.LoginExpired), states.last())
    }

    @Test
    fun pendingLogin_toStringHidesTheToken() {
        assertFalse(pending.copy(token = "s3cret").toString().contains("s3cret"))
    }

    private companion object {
        const val START_MILLIS = 1_000L
    }
}
