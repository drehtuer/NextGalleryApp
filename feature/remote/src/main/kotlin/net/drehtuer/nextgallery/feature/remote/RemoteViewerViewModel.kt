package net.drehtuer.nextgallery.feature.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteBrowseRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteDownloadProgress
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteDownloads
import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths

data class RemoteViewerUiState(
    val images: List<RemoteItem> = emptyList(),
    val initialIndex: Int? = null,
    val currentIndex: Int = 0,
    val urls: RemoteImageUrls? = null,
    val chromeVisible: Boolean = true,
    val showInfo: Boolean = false,
    val progress: RemoteDownloadProgress? = null,
    val error: AppError? = null,
) {
    val current: RemoteItem? get() = images.getOrNull(currentIndex)
}

sealed interface RemoteViewerEvent {
    data class PageChanged(val index: Int) : RemoteViewerEvent

    data object ToggleChrome : RemoteViewerEvent

    data object ShowInfo : RemoteViewerEvent

    data object HideInfo : RemoteViewerEvent
}

/** Pages through the images of the folder of [path]. Originals are only downloaded for the page on screen. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = RemoteViewerViewModel.Factory::class)
class RemoteViewerViewModel @AssistedInject constructor(
    @Assisted private val path: String,
    private val browse: RemoteBrowseRepository,
    accounts: NextcloudAccountRepository,
    private val downloads: RemoteDownloads,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(path: String): RemoteViewerViewModel
    }

    private val viewer = MutableStateFlow(RemoteViewerUiState())
    private val urls = accounts.observeImageUrls()

    private val progress = combine(viewer.map { it.current }.distinctUntilChanged(), urls) { item, urls ->
        if (item != null && urls != null) urls.original(item) else null
    }
        .distinctUntilChanged()
        .flatMapLatest { url -> if (url == null) flowOf(null) else downloads.observe(url) }

    val state: StateFlow<RemoteViewerUiState> = combine(viewer, urls, progress) { current, urls, progress ->
        current.copy(urls = urls, progress = progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RemoteViewerUiState())

    init {
        viewModelScope.launch {
            browse.list(RemotePaths.parent(path)).collect { listing ->
                val images = listing.items.filterNot { it.isDirectory }
                viewer.update { current ->
                    val index = current.initialIndex ?: images.indexOfFirst { it.path == path }.takeIf { it >= 0 }
                    current.copy(
                        images = images,
                        initialIndex = index,
                        currentIndex = if (current.initialIndex == null) index ?: 0 else current.currentIndex,
                        error = listing.error.takeIf { images.isEmpty() },
                    )
                }
            }
        }
    }

    fun onEvent(event: RemoteViewerEvent) {
        when (event) {
            is RemoteViewerEvent.PageChanged -> viewer.update { it.copy(currentIndex = event.index) }
            RemoteViewerEvent.ToggleChrome -> viewer.update { it.copy(chromeVisible = !it.chromeVisible) }
            RemoteViewerEvent.ShowInfo -> viewer.update { it.copy(showInfo = true) }
            RemoteViewerEvent.HideInfo -> viewer.update { it.copy(showInfo = false) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
