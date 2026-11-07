package net.drehtuer.nextgallery.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coVerify
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
import net.drehtuer.nextgallery.core.data.nextcloud.TrustedCertificateRepository
import net.drehtuer.nextgallery.core.model.CertificateInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrustedCertificatesTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val info = CertificateInfo("cloud.example.com", "CN=a", "CN=b", 1, 2, "AA:BB")

    @Test
    fun screen_empty_explains() {
        composeRule.setContent { TrustedCertificatesScreen(emptyList(), onRemove = {}, onBack = {}) }

        composeRule.onNodeWithText("You haven't accepted any certificates.").assertIsDisplayed()
    }

    @Test
    fun screen_showsHostAndFingerprint() {
        composeRule.setContent { TrustedCertificatesScreen(listOf(info), onRemove = {}, onBack = {}) }

        composeRule.onNodeWithText("cloud.example.com").assertIsDisplayed()
        composeRule.onNodeWithText("AA:BB").assertIsDisplayed()
    }

    @Test
    fun screen_removeClicked_removesThatHost() {
        var removed: String? = null
        composeRule.setContent { TrustedCertificatesScreen(listOf(info), onRemove = { removed = it }, onBack = {}) }

        composeRule.onNodeWithText("Remove").performClick()

        assertEquals("cloud.example.com", removed)
    }

    private val repository = mockk<TrustedCertificateRepository>(relaxed = true) {
        every { observe() } returns flowOf(listOf(info))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun resetMain() = Dispatchers.resetMain()

    @Test
    fun viewModel_listsTheCertificates() = runTest {
        val viewModel = TrustedCertificatesViewModel(repository)

        assertEquals(listOf(info), viewModel.state.first { it.isNotEmpty() })
    }

    @Test
    fun viewModel_remove_removesFromTheRepository() = runTest {
        TrustedCertificatesViewModel(repository).remove("cloud.example.com")

        coVerify { repository.remove("cloud.example.com") }
    }
}
