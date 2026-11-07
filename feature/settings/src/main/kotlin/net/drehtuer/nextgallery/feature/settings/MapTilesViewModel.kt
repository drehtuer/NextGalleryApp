package net.drehtuer.nextgallery.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.data.settings.TileUrls
import net.drehtuer.nextgallery.core.model.UserSettings
import javax.inject.Inject

/** [url] is what the field shows: the user's edit, or else the stored address. */
data class MapTilesUiState(val url: String = UserSettings.DEFAULT_TILE_URL, val edited: Boolean = false) {
    val valid: Boolean
        get() = TileUrls.isValid(url)
}

sealed interface MapTilesEvent {
    data class UrlChanged(val url: String) : MapTilesEvent

    data object Save : MapTilesEvent

    data object UseDefault : MapTilesEvent
}

@HiltViewModel
class MapTilesViewModel @Inject constructor(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val edit = MutableStateFlow<String?>(null)

    val state: StateFlow<MapTilesUiState> = combine(settingsRepository.observeSettings(), edit) { settings, edit ->
        MapTilesUiState(url = edit ?: settings.tileUrl, edited = edit != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MapTilesUiState())

    fun onEvent(event: MapTilesEvent) {
        when (event) {
            is MapTilesEvent.UrlChanged -> edit.value = event.url
            MapTilesEvent.Save -> edit.value?.takeIf(TileUrls::isValid)?.let(::store)
            MapTilesEvent.UseDefault -> store(UserSettings.DEFAULT_TILE_URL)
        }
    }

    private fun store(url: String) {
        edit.value = null
        viewModelScope.launch { settingsRepository.setTileUrl(url) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
