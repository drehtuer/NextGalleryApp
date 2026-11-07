package net.drehtuer.nextgallery.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.ThemeMode
import net.drehtuer.nextgallery.core.model.UserSettings
import javax.inject.Inject

data class AppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val gridColumns: Int = UserSettings.DEFAULT_GRID_COLUMNS,
    val accentColor: AccentColor = AccentColor.BLUE,
)

sealed interface AppearanceEvent {
    data class ThemeModeSelected(val mode: ThemeMode) : AppearanceEvent

    data class GridColumnsSelected(val columns: Int) : AppearanceEvent

    data class AccentSelected(val color: AccentColor) : AppearanceEvent
}

@HiltViewModel
class AppearanceViewModel @Inject constructor(private val settingsRepository: SettingsRepository) : ViewModel() {
    val state: StateFlow<AppearanceUiState> = settingsRepository.observeSettings()
        .map { AppearanceUiState(it.themeMode, it.gridColumns, it.accentColor) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppearanceUiState())

    fun onEvent(event: AppearanceEvent) {
        viewModelScope.launch {
            when (event) {
                is AppearanceEvent.ThemeModeSelected -> settingsRepository.setThemeMode(event.mode)
                is AppearanceEvent.GridColumnsSelected -> settingsRepository.setGridColumns(event.columns)
                is AppearanceEvent.AccentSelected -> settingsRepository.setAccentColor(event.color)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
