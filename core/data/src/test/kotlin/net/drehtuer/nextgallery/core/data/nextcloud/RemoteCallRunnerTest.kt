package net.drehtuer.nextgallery.core.data.nextcloud

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

class RemoteCallRunnerTest {
    @Test
    fun call_loggedOut_returnsNotLoggedInWithoutRunningTheCall() = runTest {
        var called = false

        val result = testRunner(FakeAccountStore(null)).call { called = true }

        assertEquals(AppResult.Failure(AppError.NotLoggedIn), result)
        assertFalse(called)
    }

    @Test
    fun call_loggedIn_passesTheAccountAndReturnsTheResult() = runTest {
        assertEquals(AppResult.Success(TEST_ACCOUNT.userId), testRunner().call { it.userId })
    }

    @Test
    fun call_httpFailure_isMapped() = runTest {
        assertEquals(AppResult.Failure(AppError.Auth), testRunner().call { throw NextcloudHttpException(401) })
    }

    @Test
    fun call_networkFailureOnLocalAddressWithoutPermission_isBlocked() = runTest {
        val denied = object : LocalNetworkPermissionSource {
            override fun isGranted() = false
        }
        val policy = LocalNetworkPolicy(denied) { listOf(InetAddress.getByName("192.168.1.5")) }
        val runner = RemoteCallRunner(FakeAccountStore(), policy)

        assertEquals(AppResult.Failure(AppError.LocalNetworkBlocked), runner.call { throw IOException("timeout") })
    }

    @Test
    fun call_cancellation_isRethrown() {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { testRunner().call { throw CancellationException() } }
        }
    }

    @Test
    fun currentAccount_returnsTheSignedInAccount() = runTest {
        assertEquals(TEST_ACCOUNT, testRunner().currentAccount())
    }

    @Test
    fun call_forAnOwnerWhoLeftBefore_doesNotRunTheCall() = runTest {
        var called = false

        val result = testRunner(FakeAccountStore(null)).call(TEST_ACCOUNT) { called = true }

        assertEquals(AppResult.Failure(AppError.NotLoggedIn), result)
        assertFalse(called)
    }

    @Test
    fun call_ownerLeavesDuringTheCall_discardsTheResult() = runTest {
        val store = FakeAccountStore()

        val result = testRunner(store).call(TEST_ACCOUNT) {
            store.account.value = null
            "secret of the old account"
        }

        assertEquals(AppResult.Failure(AppError.NotLoggedIn), result)
    }

    @Test
    fun call_anotherAccountSignsInDuringTheCall_discardsTheResult() = runTest {
        val store = FakeAccountStore()

        val result = testRunner(store).call(TEST_ACCOUNT) {
            store.account.value = TEST_ACCOUNT.copy(userId = "someone-else")
            "secret of the old account"
        }

        assertEquals(AppResult.Failure(AppError.NotLoggedIn), result)
    }

    @Test
    fun call_ownerGetsANewPasswordDuringTheCall_keepsTheResult() = runTest {
        val store = FakeAccountStore()

        val result = testRunner(store).call(TEST_ACCOUNT) {
            store.account.value = TEST_ACCOUNT.copy(appPassword = "new")
            "listing"
        }

        assertEquals(AppResult.Success("listing"), result)
    }

    @Test
    fun call_accountChangesDuringACallThatFails_reportsNotLoggedIn() = runTest {
        val store = FakeAccountStore()

        val result = testRunner(store).call {
            store.account.value = null
            throw NextcloudHttpException(401)
        }

        assertEquals(AppResult.Failure(AppError.NotLoggedIn), result)
    }
}
