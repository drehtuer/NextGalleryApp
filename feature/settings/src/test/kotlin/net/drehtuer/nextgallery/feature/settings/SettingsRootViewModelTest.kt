package net.drehtuer.nextgallery.feature.settings

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRootViewModelTest {
    private val repository = mockk<NextcloudAccountRepository>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun accountLabel_noAccount_isNull() = runTest {
        every { repository.observeAccount() } returns flowOf(null)

        assertNull(SettingsRootViewModel(repository).accountLabel.first())
    }

    @Test
    fun accountLabel_withAccount_isLoginAtHost() = runTest {
        val account = NextcloudAccount("https://cloud.example.com", "anna", "id", "pw")
        every { repository.observeAccount() } returns flowOf(account)

        val label = SettingsRootViewModel(repository).accountLabel.first { it != null }

        assertEquals("anna@cloud.example.com", label)
    }
}
