package net.drehtuer.nextgallery.core.data.nextcloud

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.nextcloud.ConnectionEvictor
import net.drehtuer.nextgallery.core.nextcloud.LoginExpiredException
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowResult
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowSession
import net.drehtuer.nextgallery.core.nextcloud.LoginFlowV2Client
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import net.drehtuer.nextgallery.core.nextcloud.ServerUrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

class DefaultNextcloudAccountRepositoryTest {
    private val loginFlow = mockk<LoginFlowV2Client>()
    private val ocs = mockk<OcsClient>(relaxed = true)
    private val store = FakeAccountStore(null)
    private val cache = mockk<RemoteCacheStore>(relaxed = true)
    private val images = mockk<RemoteImageCache>(relaxed = true)
    private val shares = mockk<RemoteShareCache>(relaxed = true)
    private val connections = mockk<ConnectionEvictor>(relaxed = true)
    private val policy = LocalNetworkPolicy(GrantedLocalNetwork) { emptyList() }

    private val session = LoginFlowSession("https://cloud.example.com/login", "https://cloud.example.com/poll", "token")
    private val flowResult = LoginFlowResult("https://cloud.example.com", "anna", "app-password")

    private fun repository(networkPolicy: LocalNetworkPolicy = policy) = DefaultNextcloudAccountRepository(
        normalizer = ServerUrlNormalizer(allowCleartext = false),
        loginFlow = loginFlow,
        ocs = ocs,
        accounts = store,
        wiper = RemoteDataWiper(cache, images, shares, connections),
        localNetwork = networkPolicy,
    )

    private fun loginSucceeds() {
        coEvery { loginFlow.start("https://cloud.example.com") } returns session
        coEvery { loginFlow.poll(session) } returns flowResult
        coEvery { ocs.userId("https://cloud.example.com", "anna", "app-password") } returns "anna-id"
    }

    @Test
    fun login_success_emitsStartedPollingSuccess() = runTest {
        loginSucceeds()

        val states = repository().login("cloud.example.com").toList()

        assertEquals(
            listOf(
                LoginState.Started(session.loginUrl),
                LoginState.Polling,
                LoginState.Success(TEST_ACCOUNT),
            ),
            states,
        )
    }

    @Test
    fun login_success_storesTheAccount() = runTest {
        loginSucceeds()

        repository().login("cloud.example.com").toList()

        assertEquals(TEST_ACCOUNT, store.account.value)
    }

    @Test
    fun login_invalidUrl_failsWithoutNetwork() = runTest {
        val states = repository().login("http://cloud.example.com").toList()

        assertEquals(listOf<LoginState>(LoginState.Failed(AppError.InvalidServerUrl)), states)
        coVerify(exactly = 0) { loginFlow.start(any()) }
    }

    @Test
    fun login_startFails_emitsFailed() = runTest {
        coEvery { loginFlow.start(any()) } throws IOException("no route")

        val states = repository().login("cloud.example.com").toList()

        assertEquals(listOf<LoginState>(LoginState.Failed(AppError.Network("no route"))), states)
    }

    @Test
    fun login_expired_emitsFailedAndStoresNothing() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.poll(session) } throws LoginExpiredException()

        val states = repository().login("cloud.example.com").toList()

        assertEquals(LoginState.Failed(AppError.LoginExpired), states.last())
        assertNull(store.account.value)
    }

    @Test
    fun login_userIdFails_storesNothing() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.poll(session) } returns flowResult
        coEvery { ocs.userId(any(), any(), any()) } throws IOException("gone")

        repository().login("cloud.example.com").toList()

        assertNull(store.account.value)
    }

    @Test
    fun login_blockedLocalAddress_emitsBlocked() = runTest {
        val denied = object : LocalNetworkPermissionSource {
            override fun isGranted() = false
        }
        val local = LocalNetworkPolicy(denied) { listOf(InetAddress.getByName("192.168.1.5")) }
        coEvery { loginFlow.start(any()) } throws IOException("timeout")

        val states = repository(local).login("192.168.1.5").toList()

        assertEquals(listOf<LoginState>(LoginState.Failed(AppError.LocalNetworkBlocked)), states)
    }

    @Test
    fun login_sameAccountAgain_keepsTheCache() = runTest {
        store.account.value = TEST_ACCOUNT.copy(appPassword = "old")
        loginSucceeds()

        repository().login("cloud.example.com").toList()

        coVerify(exactly = 0) { cache.clear() }
    }

    @Test
    fun login_anotherAccount_wipesTheCache() = runTest {
        store.account.value = TEST_ACCOUNT.copy(userId = "someone-else")
        loginSucceeds()

        repository().login("cloud.example.com").toList()

        coVerify { cache.clear() }
    }

    @Test
    fun login_cancelled_storesNothing() = runTest {
        coEvery { loginFlow.start(any()) } returns session
        coEvery { loginFlow.poll(session) } coAnswers { kotlinx.coroutines.awaitCancellation() }

        repository().login("cloud.example.com").test {
            awaitItem()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(store.account.value)
    }

    @Test
    fun logout_revokesTheAppPassword() = runTest {
        store.account.value = TEST_ACCOUNT

        repository().logout()

        coVerify { ocs.revokeAppPassword("https://cloud.example.com", "anna", "app-password") }
    }

    @Test
    fun logout_revokesBeforeClearing() = runTest {
        val spyStore = mockk<AccountStore>(relaxed = true)
        io.mockk.every { spyStore.observeAccount() } returns kotlinx.coroutines.flow.flowOf(TEST_ACCOUNT)
        val repository = DefaultNextcloudAccountRepository(
            ServerUrlNormalizer(false),
            loginFlow,
            ocs,
            spyStore,
            RemoteDataWiper(cache, images, shares, connections),
            policy,
        )

        repository.logout()

        coVerifyOrder {
            ocs.revokeAppPassword(any(), any(), any())
            spyStore.clear()
        }
    }

    @Test
    fun logout_clearsTheAccount() = runTest {
        store.account.value = TEST_ACCOUNT

        repository().logout()

        assertNull(store.account.value)
    }

    @Test
    fun logout_clearsCachesAndConnections() = runTest {
        store.account.value = TEST_ACCOUNT

        repository().logout()

        coVerify { cache.clear() }
        coVerify { images.clear() }
        verify { shares.clear() }
        verify { connections.evictConnections() }
    }

    @Test
    fun logout_serverUnreachable_stillClearsEverything() = runTest {
        store.account.value = TEST_ACCOUNT
        coEvery { ocs.revokeAppPassword(any(), any(), any()) } throws IOException("offline")

        repository().logout()

        assertNull(store.account.value)
        coVerify { cache.clear() }
    }

    @Test
    fun logout_oneCacheFails_stillClearsTheOthers() = runTest {
        store.account.value = TEST_ACCOUNT
        coEvery { cache.clear() } throws IOException("disk")

        repository().logout()

        coVerify { images.clear() }
    }

    @Test
    fun logout_notLoggedIn_sendsNothing() = runTest {
        repository().logout()

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), any()) }
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
}
