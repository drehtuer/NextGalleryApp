package net.drehtuer.nextgallery.feature.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.nextcloud.ConnectivitySource
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteBrowseRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFileOperations
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteListing
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteMoveFlow
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareRepository
import net.drehtuer.nextgallery.core.data.nextcloud.TrustedCertificateRepository
import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemotePaths
import javax.inject.Inject

/** Everything the browser needs from the data layer. */
class RemoteBrowserDependencies @Inject constructor(
    val accounts: NextcloudAccountRepository,
    val browse: RemoteBrowseRepository,
    val operations: RemoteFileOperations,
    val moveFlow: RemoteMoveFlow,
    val shares: RemoteShareRepository,
    val certificates: TrustedCertificateRepository,
    val connectivity: ConnectivitySource,
)

@HiltViewModel
class RemoteBrowserViewModel @Inject constructor(private val dependencies: RemoteBrowserDependencies) : ViewModel() {
    private val effectChannel = Channel<RemoteBrowserEffect>(Channel.BUFFERED)
    private val browser = MutableStateFlow(RemoteBrowserUiState())
    private var listingJob: Job? = null

    private val move = RemoteMoveController(
        scope = viewModelScope,
        operations = dependencies.operations,
        flow = dependencies.moveFlow,
        sendEffect = ::send,
        onServerChanged = ::onServerChanged,
    )
    private val actions = RemoteActionsController(
        scope = viewModelScope,
        operations = dependencies.operations,
        shares = dependencies.shares,
        sendEffect = ::send,
        onServerChanged = ::onServerChanged,
    )

    private val selection = RemoteSelectionController(
        browser = browser,
        sendEffect = ::send,
        startMove = { items -> move.open(items, browser.value.path) },
        share = { items -> actions.share(items) },
        delete = { items -> actions.delete(items) },
    )

    val effects: Flow<RemoteBrowserEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<RemoteBrowserUiState> = combine(
        browser,
        move.state,
        actions.share,
        actions.deleteConfirmation,
    ) { current, moving, share, deleteConfirmation ->
        current.copy(move = moving, share = share, deleteConfirmation = deleteConfirmation)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RemoteBrowserUiState())

    init {
        viewModelScope.launch {
            dependencies.connectivity.isOnline.collect { online -> browser.update { it.copy(isConnected = online) } }
        }
        viewModelScope.launch { dependencies.accounts.observeImageUrls().collect(::onAccountChanged) }
    }

    fun onEvent(event: RemoteBrowserEvent) {
        when (event) {
            is BrowseEvent -> onBrowseEvent(event)
            is SelectionEvent -> selection.onEvent(event)
            is MoveEvent -> move.onEvent(event)
            is ActionEvent -> actions.onEvent(event)
        }
    }

    private fun send(effect: RemoteBrowserEffect) {
        effectChannel.trySend(effect)
    }

    private fun onAccountChanged(urls: RemoteImageUrls?) {
        actions.cancelDelete()
        if (urls == null) {
            listingJob?.cancel()
            browser.update {
                RemoteBrowserUiState(loginRequired = true, isConnected = it.isConnected, isLoading = false)
            }
        } else {
            browser.update { it.copy(loginRequired = false, urls = urls, sessionEnded = false) }
            load()
        }
    }

    private fun onBrowseEvent(event: BrowseEvent) {
        when (event) {
            is BrowseEvent.OpenFolder -> openFolder(event.path)
            BrowseEvent.Up -> openFolder(RemotePaths.parent(browser.value.path))
            BrowseEvent.Refresh -> load()
            BrowseEvent.TrustCertificate -> trustCertificate()
            BrowseEvent.RejectCertificate -> browser.update { it.copy(certificate = null) }
        }
    }

    private fun openFolder(path: String) {
        browser.update {
            it.copy(path = path, folders = emptyList(), images = emptyList(), isLoading = true, error = null)
        }
        load()
    }

    private fun load() {
        listingJob?.cancel()
        val path = browser.value.path
        listingJob = viewModelScope.launch { dependencies.browse.list(path).collect(::onListing) }
    }

    private fun onListing(listing: RemoteListing) {
        val (folders, images) = listing.items.partition { it.isDirectory }
        browser.update { current ->
            current.copy(
                folders = folders,
                images = images,
                isLoading = listing.isRefreshing && listing.items.isEmpty(),
                isRefreshing = listing.isRefreshing,
                serverUnreachable = listing.error is AppError.Network,
                sessionEnded = listing.error == AppError.Auth,
                loginRequired = listing.error == AppError.NotLoggedIn,
                certificate = (listing.error as? AppError.UntrustedCertificate)?.info,
                error = listing.error.takeUnless { it is AppError.Network || it == AppError.Auth },
            )
        }
        // Without items the error fills the screen; with cached items it would otherwise go unnoticed.
        val current = browser.value
        if (listing.items.isNotEmpty() && current.certificate == null) {
            current.error?.let { send(RemoteBrowserEffect.Failed(it)) }
        }
    }

    /** Only here, after the user's explicit decision, does a certificate get pinned (D32). */
    private fun trustCertificate() {
        val info = browser.value.certificate ?: return
        browser.update { it.copy(certificate = null, error = null) }
        viewModelScope.launch {
            when (val result = dependencies.certificates.accept(info)) {
                is AppResult.Success -> load()
                is AppResult.Failure -> browser.update { it.copy(error = result.error) }
            }
        }
    }

    private fun onServerChanged() {
        selection.clear()
        load()
    }
}
