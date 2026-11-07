package net.drehtuer.nextgallery.core.data.upload

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeWorkScheduler
import net.drehtuer.nextgallery.core.data.nextcloud.FakeAccountStore
import net.drehtuer.nextgallery.core.data.nextcloud.TEST_ACCOUNT
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadStateSyncObserverTest {
    private val accounts = FakeAccountStore(null)
    private val settings = MutableStateFlow(UserSettings())
    private val repository = mockk<SettingsRepository> { every { observeSettings() } returns settings }
    private val scheduler = FakeWorkScheduler()

    private fun TestScope.start() {
        backgroundScope.launch { UploadStateSyncObserver(accounts, repository, scheduler).observe() }
        runCurrent()
    }

    @Test
    fun observe_schedulesThePeriodicCheck() = runTest {
        start()

        assertEquals(1, scheduler.uploadStateSchedules)
    }

    @Test
    fun observe_appStartsLoggedIn_requestsNoExtraCheck() = runTest {
        accounts.account.value = TEST_ACCOUNT

        start()

        assertEquals(0, scheduler.uploadStateRequests)
    }

    @Test
    fun observe_login_requestsACheck() = runTest {
        start()

        accounts.account.value = TEST_ACCOUNT
        runCurrent()

        assertEquals(1, scheduler.uploadStateRequests)
    }

    @Test
    fun observe_logout_requestsNoCheck() = runTest {
        accounts.account.value = TEST_ACCOUNT
        start()

        accounts.account.value = null
        runCurrent()

        assertEquals(0, scheduler.uploadStateRequests)
    }

    @Test
    fun observe_newRemoteRoot_requestsACheck() = runTest {
        accounts.account.value = TEST_ACCOUNT
        start()

        settings.value = UserSettings(remoteRoot = "/Photos")
        runCurrent()

        assertEquals(1, scheduler.uploadStateRequests)
    }

    @Test
    fun observe_newRemoteRootWhileLoggedOut_requestsNoCheck() = runTest {
        start()

        settings.value = UserSettings(remoteRoot = "/Photos")
        runCurrent()

        assertEquals(0, scheduler.uploadStateRequests)
    }

    @Test
    fun observe_otherSettingChanged_requestsNoCheck() = runTest {
        accounts.account.value = TEST_ACCOUNT
        start()

        settings.value = UserSettings(gridColumns = 4)
        runCurrent()

        assertEquals(0, scheduler.uploadStateRequests)
    }

    @Test
    fun observe_newPasswordForTheSameAccount_requestsNoCheck() = runTest {
        accounts.account.value = TEST_ACCOUNT
        start()

        accounts.account.value = TEST_ACCOUNT.copy(appPassword = "new")
        runCurrent()

        assertEquals(0, scheduler.uploadStateRequests)
    }
}
