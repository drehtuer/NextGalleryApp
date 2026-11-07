package net.drehtuer.nextgallery.feature.settings

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapTilesViewModelTest {
    private val storedUrl = "https://stored.example.org/{z}/{x}/{y}.png"
    private val newUrl = "https://new.example.org/{z}/{x}/{y}.png"
    private val repository = mockk<SettingsRepository> {
        every { observeSettings() } returns flowOf(UserSettings(tileUrl = storedUrl))
        coEvery { setTileUrl(any()) } returns AppResult.Success(Unit)
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun state_noEdit_showsStoredAddress() = runTest {
        MapTilesViewModel(repository).state.test {
            assertEquals(MapTilesUiState(storedUrl, edited = false), expectMostRecentItem())
        }
    }

    @Test
    fun urlChanged_showsTheEdit() = runTest {
        val viewModel = MapTilesViewModel(repository)

        viewModel.onEvent(MapTilesEvent.UrlChanged(newUrl))

        viewModel.state.test {
            assertEquals(MapTilesUiState(newUrl, edited = true), expectMostRecentItem())
        }
    }

    @Test
    fun save_validEdit_isStored() {
        val viewModel = MapTilesViewModel(repository)
        viewModel.onEvent(MapTilesEvent.UrlChanged(newUrl))

        viewModel.onEvent(MapTilesEvent.Save)

        coVerify { repository.setTileUrl(newUrl) }
    }

    @Test
    fun save_invalidEdit_isNotStored() {
        val viewModel = MapTilesViewModel(repository)
        viewModel.onEvent(MapTilesEvent.UrlChanged("http://insecure.example.org"))

        viewModel.onEvent(MapTilesEvent.Save)

        coVerify(exactly = 0) { repository.setTileUrl(any()) }
    }

    @Test
    fun save_noEdit_storesNothing() {
        MapTilesViewModel(repository).onEvent(MapTilesEvent.Save)

        coVerify(exactly = 0) { repository.setTileUrl(any()) }
    }

    @Test
    fun useDefault_storesOpenStreetMap() {
        MapTilesViewModel(repository).onEvent(MapTilesEvent.UseDefault)

        coVerify { repository.setTileUrl(UserSettings.DEFAULT_TILE_URL) }
    }

    @Test
    fun uiState_validAddress_isValid() {
        assertTrue(MapTilesUiState(newUrl).valid)
    }

    @Test
    fun uiState_invalidAddress_isNotValid() {
        assertFalse(MapTilesUiState("nonsense").valid)
    }
}
