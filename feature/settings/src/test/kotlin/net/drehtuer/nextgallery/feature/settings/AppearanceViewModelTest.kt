package net.drehtuer.nextgallery.feature.settings

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
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    private val settings = UserSettings(gridColumns = 5, themeMode = ThemeMode.DARK, accentColor = AccentColor.PLUM)
    private val repository = mockk<SettingsRepository>(relaxed = true) {
        every { observeSettings() } returns flowOf(settings)
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun state_comesFromSettings() = runTest {
        val viewModel = AppearanceViewModel(repository)

        val state = viewModel.state.first { it != AppearanceUiState() }

        assertEquals(AppearanceUiState(ThemeMode.DARK, 5, AccentColor.PLUM), state)
    }

    @Test
    fun onEvent_themeModeSelected_storesMode() {
        AppearanceViewModel(repository).onEvent(AppearanceEvent.ThemeModeSelected(ThemeMode.LIGHT))

        coVerify { repository.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun onEvent_gridColumnsSelected_storesColumns() {
        AppearanceViewModel(repository).onEvent(AppearanceEvent.GridColumnsSelected(4))

        coVerify { repository.setGridColumns(4) }
    }

    @Test
    fun onEvent_accentSelected_storesColor() {
        AppearanceViewModel(repository).onEvent(AppearanceEvent.AccentSelected(AccentColor.MOSS))

        coVerify { repository.setAccentColor(AccentColor.MOSS) }
    }
}
