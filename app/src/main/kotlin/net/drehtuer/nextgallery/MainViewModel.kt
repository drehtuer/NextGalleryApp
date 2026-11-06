package net.drehtuer.nextgallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.access.MediaAccessRepository
import net.drehtuer.nextgallery.core.data.index.IndexScheduler
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.model.MediaAccess
import javax.inject.Inject

/** `access` is null until it was checked for the first time. */
data class MainUiState(val access: MediaAccess? = null, val indexedCount: Int = 0)

sealed interface MainEvent {
    data object Resumed : MainEvent

    /** The permission dialog or photo selection closed; the selection may have changed even if access did not. */
    data object AccessRequestFinished : MainEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaAccessRepository: MediaAccessRepository,
    mediaRepository: MediaRepository,
    private val indexScheduler: IndexScheduler,
) : ViewModel() {
    private val access = MutableStateFlow<MediaAccess?>(null)
    private var mediaChangeObserver: Job? = null

    val requestedPermissions: List<String> = mediaAccessRepository.requestedPermissions

    val state: StateFlow<MainUiState> = combine(access, mediaRepository.observeIndexedCount(), ::MainUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MainUiState())

    fun onEvent(event: MainEvent) {
        val previous = access.value
        val current = mediaAccessRepository.currentAccess()
        access.value = current
        viewModelScope.launch {
            when {
                previous == null -> indexScheduler.onAppStart(current)

                event == MainEvent.AccessRequestFinished || previous != current -> indexScheduler.onAccessChanged(
                    current,
                )
            }
        }
        if (current != MediaAccess.DENIED && mediaChangeObserver == null) {
            mediaChangeObserver = viewModelScope.launch { indexScheduler.observeMediaChanges() }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
