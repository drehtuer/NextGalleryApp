package net.drehtuer.nextgallery.core.data.nextcloud

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.nextcloud.ConnectionEvictor
import net.drehtuer.nextgallery.core.nextcloud.OcsClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionTest {
    private val ocs = mockk<OcsClient>(relaxed = true)
    private val store = FakeAccountStore(TEST_ACCOUNT)
    private val cache = mockk<RemoteCacheStore>(relaxed = true)
    private val images = mockk<RemoteImageCache>(relaxed = true)
    private val shares = mockk<RemoteShareCache>(relaxed = true)
    private val connections = mockk<ConnectionEvictor>(relaxed = true)

    private val newPassword = TEST_ACCOUNT.copy(appPassword = "new-password")
    private val otherUser = TEST_ACCOUNT.copy(userId = "someone-else", appPassword = "new-password")

    private fun session(accounts: AccountStore = store) = AccountSession(
        accounts,
        RemoteDataWiper(cache, images, shares, connections),
        AppPasswordRevoker(ocs, CoroutineScope(Dispatchers.Unconfined)),
    )

    private val session = session()

    private fun revokedOldPassword() = coVerify {
        ocs.revokeAppPassword("https://cloud.example.com", "anna", "app-password")
    }

    @Test
    fun logOut_clearsTheAccount() = runTest {
        session.logOut()

        assertNull(store.account.value)
    }

    @Test
    fun logOut_clearsCachesAndConnections() = runTest {
        session.logOut()

        coVerify { cache.clear() }
        coVerify { images.clear() }
        verify { shares.clear() }
        verify { connections.evictConnections() }
    }

    @Test
    fun logOut_revokesWithTheAccountItHadBefore() = runTest {
        session.logOut()

        revokedOldPassword()
    }

    @Test
    fun logOut_revokesOnlyAfterTheLocalDataIsGone() = runTest {
        var accountAtRevoke = store.account.value
        var wipedAtRevoke = false
        coEvery { cache.clear() } coAnswers { wipedAtRevoke = true }
        coEvery { ocs.revokeAppPassword(any(), any(), any()) } coAnswers {
            accountAtRevoke = store.account.value
            assertTrue(wipedAtRevoke)
        }

        session.logOut()

        assertNull(accountAtRevoke)
        assertTrue(wipedAtRevoke)
    }

    @Test
    fun logOut_doesNotWaitForTheServer() = runTest {
        coEvery { ocs.revokeAppPassword(any(), any(), any()) } coAnswers { awaitCancellation() }

        session.logOut()

        assertNull(store.account.value)
    }

    @Test
    fun logOut_serverUnreachable_stillEndsLoggedOut() = runTest {
        coEvery { ocs.revokeAppPassword(any(), any(), any()) } throws IOException("offline")

        session.logOut()

        assertNull(store.account.value)
        coVerify { cache.clear() }
    }

    @Test
    fun logOut_clearFails_stillWipesAndRevokes() = runTest {
        val failing = mockk<AccountStore> {
            every { observeAccount() } returns flowOf(TEST_ACCOUNT)
            coEvery { clear() } throws IOException("disk")
        }

        session(failing).logOut()

        coVerify { cache.clear() }
        revokedOldPassword()
    }

    @Test
    fun logOut_readFails_stillClearsAndWipes() = runTest {
        val failing = mockk<AccountStore>(relaxed = true) {
            every { observeAccount() } returns flow { throw IOException("disk") }
        }

        session(failing).logOut()

        coVerify { failing.clear() }
        coVerify { cache.clear() }
    }

    @Test
    fun logOut_oneCacheFails_stillClearsTheOthers() = runTest {
        coEvery { cache.clear() } throws IOException("disk")

        session.logOut()

        coVerify { images.clear() }
    }

    @Test
    fun logOut_notLoggedIn_sendsNothing() = runTest {
        store.account.value = null

        session.logOut()

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), any()) }
    }

    @Test
    fun logOut_callerCancelledHalfway_stillFinishes() = runTest {
        val cacheClearing = CompletableDeferred<Unit>()
        coEvery { cache.clear() } coAnswers { cacheClearing.await() }
        val caller = launch(UnconfinedTestDispatcher(testScheduler)) { session.logOut() }

        caller.cancel()
        cacheClearing.complete(Unit)
        caller.join()

        coVerify { images.clear() }
        revokedOldPassword()
    }

    @Test
    fun logOut_callerCancelled_revokeGoesOn() = runTest {
        val serverAnswering = CompletableDeferred<Unit>()
        var revoked = false
        coEvery { ocs.revokeAppPassword(any(), any(), any()) } coAnswers {
            serverAnswering.await()
            revoked = true
        }
        val caller = launch(UnconfinedTestDispatcher(testScheduler)) { session.logOut() }

        caller.cancel()
        serverAnswering.complete(Unit)

        assertTrue(revoked)
    }

    @Test
    fun logOut_waitsForAWriteThatIsRunning() = runTest {
        val writing = CompletableDeferred<Unit>()
        val writer =
            launch(UnconfinedTestDispatcher(testScheduler)) { session.writeFor(TEST_ACCOUNT) { writing.await() } }
        val logout = launch(UnconfinedTestDispatcher(testScheduler)) { session.logOut() }

        coVerify(exactly = 0) { cache.clear() }

        writing.complete(Unit)
        writer.join()
        logout.join()
        coVerify { cache.clear() }
    }

    @Test
    fun replaceAccount_storesTheAccount() = runTest {
        session.replaceAccount(otherUser)

        assertEquals(otherUser, store.account.value)
    }

    @Test
    fun replaceAccount_nothingReadable_wipes() = runTest {
        store.account.value = null

        session.replaceAccount(TEST_ACCOUNT)

        coVerify { cache.clear() }
    }

    @Test
    fun replaceAccount_sameUser_keepsTheData() = runTest {
        session.replaceAccount(newPassword)

        coVerify(exactly = 0) { cache.clear() }
    }

    @Test
    fun replaceAccount_anotherUser_wipes() = runTest {
        session.replaceAccount(otherUser)

        coVerify { cache.clear() }
    }

    @Test
    fun replaceAccount_sameUserIdOnAnotherServer_wipes() = runTest {
        session.replaceAccount(TEST_ACCOUNT.copy(serverUrl = "https://other.example.com"))

        coVerify { cache.clear() }
    }

    @Test
    fun replaceAccount_revokesThePasswordItReplaces() = runTest {
        session.replaceAccount(newPassword)

        revokedOldPassword()
    }

    @Test
    fun replaceAccount_samePassword_doesNotRevoke() = runTest {
        session.replaceAccount(TEST_ACCOUNT)

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), any()) }
    }

    @Test
    fun replaceAccount_nothingToReplace_doesNotRevoke() = runTest {
        store.account.value = null

        session.replaceAccount(TEST_ACCOUNT)

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), any()) }
    }

    @Test
    fun replaceAccount_saveFails_keepsThePreviousPasswordAlive() {
        val failing = mockk<AccountStore> {
            every { observeAccount() } returns flowOf(TEST_ACCOUNT)
            coEvery { save(any()) } throws IOException("disk")
        }

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { session(failing).replaceAccount(newPassword) }
        }

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), any()) }
    }

    @Test
    fun revokeIfUnused_notTheStoredPassword_revokes() = runTest {
        session.revokeIfUnused("https://cloud.example.com", "anna", "unused")

        coVerify { ocs.revokeAppPassword("https://cloud.example.com", "anna", "unused") }
    }

    @Test
    fun revokeIfUnused_theStoredPassword_doesNotRevoke() = runTest {
        session.revokeIfUnused("https://cloud.example.com", "anna", TEST_ACCOUNT.appPassword)

        coVerify(exactly = 0) { ocs.revokeAppPassword(any(), any(), any()) }
    }

    @Test
    fun writeFor_ownerSignedIn_writes() = runTest {
        var written = false

        val result = session.writeFor(TEST_ACCOUNT) { written = true }

        assertTrue(result)
        assertTrue(written)
    }

    @Test
    fun writeFor_ownerGotANewPassword_writes() = runTest {
        store.account.value = newPassword

        assertTrue(session.writeFor(TEST_ACCOUNT) {})
    }

    @Test
    fun writeFor_loggedOut_doesNotWrite() = runTest {
        store.account.value = null
        var written = false

        val result = session.writeFor(TEST_ACCOUNT) { written = true }

        assertFalse(result)
        assertFalse(written)
    }

    @Test
    fun writeFor_anotherUser_doesNotWrite() = runTest {
        store.account.value = otherUser
        var written = false

        val result = session.writeFor(TEST_ACCOUNT) { written = true }

        assertFalse(result)
        assertFalse(written)
    }
}
