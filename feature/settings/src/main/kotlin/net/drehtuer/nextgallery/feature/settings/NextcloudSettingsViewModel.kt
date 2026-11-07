package net.drehtuer.nextgallery.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.nextcloud.ACCESS_LOCAL_NETWORK_PERMISSION
import net.drehtuer.nextgallery.core.data.nextcloud.LocalNetworkPermissionSource
import net.drehtuer.nextgallery.core.data.nextcloud.LoginState
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import net.drehtuer.nextgallery.core.data.nextcloud.PendingLogin
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteFileOperations
import net.drehtuer.nextgallery.core.data.nextcloud.TrustedCertificateRepository
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.model.RemotePaths

/** The folder picker for the remote root; [folders] are the subfolders of [path]. */
data class RemoteRootPickerState(
    val path: String = RemotePaths.ROOT,
    val folders: List<RemoteItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
)

/**
 * [accountLabel] is set while signed in; with it, the account is shown instead of the form, unless [showForm] asks to
 * sign in again after the session ended.
 */
data class NextcloudSettingsUiState(
    val serverAddress: String = "",
    val isSigningIn: Boolean = false,
    val accountLabel: String? = null,
    val showForm: Boolean = accountLabel == null,
    val remoteRoot: String = RemotePaths.ROOT,
    val error: AppError? = null,
    val certificate: CertificateInfo? = null,
    val showLocalNetworkExplanation: Boolean = false,
    val rootPicker: RemoteRootPickerState? = null,
)

sealed interface NextcloudSettingsEvent {
    data class ServerAddressChanged(val address: String) : NextcloudSettingsEvent

    data object SignIn : NextcloudSettingsEvent

    data object CancelSignIn : NextcloudSettingsEvent

    /** The screen is in front again, for example after the browser; a login in progress checks right away. */
    data object Resumed : NextcloudSettingsEvent

    data object Paused : NextcloudSettingsEvent

    /** The user read why the permission is needed. */
    data object LocalNetworkExplanationAccepted : NextcloudSettingsEvent

    data object LocalNetworkExplanationDismissed : NextcloudSettingsEvent

    /** The system's permission dialog closed. A denied permission only matters for addresses on the local network. */
    data object LocalNetworkRequestFinished : NextcloudSettingsEvent

    data object TrustCertificate : NextcloudSettingsEvent

    data object RejectCertificate : NextcloudSettingsEvent

    data object LogOut : NextcloudSettingsEvent

    data object OpenRootPicker : NextcloudSettingsEvent

    data class RootPickerOpened(val path: String) : NextcloudSettingsEvent

    data object RootPickerConfirmed : NextcloudSettingsEvent

    data object RootPickerDismissed : NextcloudSettingsEvent
}

sealed interface NextcloudSettingsEffect {
    data class OpenBrowser(val url: String) : NextcloudSettingsEffect

    data class RequestPermission(val permission: String) : NextcloudSettingsEffect

    data object SignedIn : NextcloudSettingsEffect
}

@HiltViewModel(assistedFactory = NextcloudSettingsViewModel.Factory::class)
class NextcloudSettingsViewModel @AssistedInject constructor(
    @Assisted private val signInAgain: Boolean,
    private val savedState: SavedStateHandle,
    private val accounts: NextcloudAccountRepository,
    private val certificates: TrustedCertificateRepository,
    private val localNetwork: LocalNetworkPermissionSource,
    private val settings: SettingsRepository,
    private val operations: RemoteFileOperations,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(signInAgain: Boolean): NextcloudSettingsViewModel
    }

    private val effectChannel = Channel<NextcloudSettingsEffect>(Channel.BUFFERED)
    private val form = MutableStateFlow(NextcloudSettingsUiState())
    private val foreground = MutableStateFlow(true)
    private var loginJob: Job? = null
    private var formWanted = signInAgain

    val effects: Flow<NextcloudSettingsEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<NextcloudSettingsUiState> = combine(
        form,
        accounts.observeAccount(),
        settings.observeSettings(),
    ) { current, account, userSettings ->
        current.copy(
            accountLabel = account?.let { accountLabel(it.loginName, it.serverUrl) },
            showForm = account == null || formWanted,
            remoteRoot = userSettings.remoteRoot,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), NextcloudSettingsUiState())

    init {
        restorePendingLogin()?.let { pending ->
            form.update { it.copy(isSigningIn = true) }
            loginJob = viewModelScope.launch { accounts.resumeLogin(pending, foreground).collect(::onLoginState) }
        }
        if (signInAgain) {
            viewModelScope.launch {
                val server = accounts.observeAccount().first()?.serverUrl.orEmpty()
                form.update { it.copy(serverAddress = server) }
            }
        }
    }

    fun onEvent(event: NextcloudSettingsEvent) {
        when (event) {
            is NextcloudSettingsEvent.ServerAddressChanged ->
                form.update { it.copy(serverAddress = event.address, error = null) }

            NextcloudSettingsEvent.SignIn,
            NextcloudSettingsEvent.CancelSignIn,
            NextcloudSettingsEvent.LocalNetworkExplanationAccepted,
            NextcloudSettingsEvent.LocalNetworkExplanationDismissed,
            NextcloudSettingsEvent.LocalNetworkRequestFinished,
            -> onLoginEvent(event)

            NextcloudSettingsEvent.Resumed -> foreground.value = true

            NextcloudSettingsEvent.Paused -> foreground.value = false

            NextcloudSettingsEvent.TrustCertificate -> trustCertificate()

            NextcloudSettingsEvent.RejectCertificate -> rejectCertificate()

            NextcloudSettingsEvent.LogOut -> viewModelScope.launch { accounts.logout() }

            NextcloudSettingsEvent.OpenRootPicker -> openPicker(state.value.remoteRoot)

            is NextcloudSettingsEvent.RootPickerOpened -> openPicker(event.path)

            NextcloudSettingsEvent.RootPickerConfirmed -> confirmPicker()

            NextcloudSettingsEvent.RootPickerDismissed -> form.update { it.copy(rootPicker = null) }
        }
    }

    private fun onLoginEvent(event: NextcloudSettingsEvent) {
        when (event) {
            NextcloudSettingsEvent.SignIn -> if (localNetwork.isGranted()) {
                startLogin()
            } else {
                form.update { it.copy(showLocalNetworkExplanation = true, error = null) }
            }

            NextcloudSettingsEvent.LocalNetworkExplanationAccepted -> {
                form.update { it.copy(showLocalNetworkExplanation = false) }
                effectChannel.trySend(NextcloudSettingsEffect.RequestPermission(ACCESS_LOCAL_NETWORK_PERMISSION))
            }

            NextcloudSettingsEvent.LocalNetworkExplanationDismissed ->
                form.update { it.copy(showLocalNetworkExplanation = false) }

            NextcloudSettingsEvent.LocalNetworkRequestFinished -> startLogin()

            else -> {
                loginJob?.cancel()
                clearPendingLogin()
                form.update { it.copy(isSigningIn = false) }
            }
        }
    }

    private fun startLogin() {
        loginJob?.cancel()
        val address = state.value.serverAddress
        form.update { it.copy(isSigningIn = true, error = null, certificate = null) }
        loginJob = viewModelScope.launch { accounts.login(address, foreground).collect(::onLoginState) }
    }

    private suspend fun onLoginState(login: LoginState) {
        when (login) {
            is LoginState.Started -> {
                savePendingLogin(login.pending)
                effectChannel.send(NextcloudSettingsEffect.OpenBrowser(login.loginUrl))
            }

            LoginState.Polling -> Unit

            is LoginState.Success -> {
                clearPendingLogin()
                formWanted = false
                form.update { it.copy(isSigningIn = false, serverAddress = "") }
                effectChannel.send(NextcloudSettingsEffect.SignedIn)
            }

            is LoginState.Failed -> {
                clearPendingLogin()
                form.update { current ->
                    val untrusted = (login.error as? AppError.UntrustedCertificate)?.info
                    current.copy(
                        isSigningIn = false,
                        certificate = untrusted,
                        error = login.error.takeIf { untrusted == null },
                    )
                }
            }
        }
    }

    /** Kept with the screen's saved state only, for as long as the login runs; it is never written to a file. */
    private fun savePendingLogin(pending: PendingLogin) {
        savedState[KEY_SERVER] = pending.serverUrl
        savedState[KEY_ENDPOINT] = pending.pollEndpoint
        savedState[KEY_TOKEN] = pending.token
        savedState[KEY_STARTED_AT] = pending.startedAtMillis
    }

    /** The values are saved and cleared together, so the start time stands for all of them. */
    private fun restorePendingLogin(): PendingLogin? {
        val startedAt = savedState.get<Long>(KEY_STARTED_AT) ?: return null
        val (server, endpoint, token) = listOf(KEY_SERVER, KEY_ENDPOINT, KEY_TOKEN).map {
            savedState.get<String>(it).orEmpty()
        }
        return PendingLogin(server, endpoint, token, startedAt)
    }

    private fun clearPendingLogin() {
        listOf(KEY_SERVER, KEY_ENDPOINT, KEY_TOKEN, KEY_STARTED_AT).forEach { savedState.remove<Any>(it) }
    }

    /** Only here, after the user's explicit decision, does a certificate get pinned (D32). */
    private fun trustCertificate() {
        val info = state.value.certificate ?: return
        form.update { it.copy(certificate = null) }
        viewModelScope.launch {
            when (val result = certificates.accept(info)) {
                is AppResult.Success -> startLogin()
                is AppResult.Failure -> form.update { it.copy(error = result.error) }
            }
        }
    }

    private fun rejectCertificate() {
        val info = state.value.certificate ?: return
        form.update { it.copy(certificate = null, error = AppError.UntrustedCertificate(info)) }
    }

    private fun openPicker(path: String) {
        form.update { it.copy(rootPicker = RemoteRootPickerState(path)) }
        viewModelScope.launch {
            val picker = when (val result = operations.listFolder(path)) {
                is AppResult.Success -> RemoteRootPickerState(path, result.value.folders, isLoading = false)
                is AppResult.Failure -> RemoteRootPickerState(path, isLoading = false, error = result.error)
            }
            form.update { current -> current.copy(rootPicker = picker.takeIf { current.rootPicker?.path == path }) }
        }
    }

    private fun confirmPicker() {
        val path = state.value.rootPicker?.path ?: return
        form.update { it.copy(rootPicker = null) }
        viewModelScope.launch { settings.setRemoteRoot(path) }
    }

    private companion object {
        const val KEY_SERVER = "login_server"
        const val KEY_ENDPOINT = "login_endpoint"
        const val KEY_TOKEN = "login_token"
        const val KEY_STARTED_AT = "login_started_at"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
