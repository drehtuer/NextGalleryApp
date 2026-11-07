package net.drehtuer.nextgallery.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import javax.inject.Inject

/** The secondary line of the Nextcloud row: the account, or null if there is none. */
@HiltViewModel
class SettingsRootViewModel @Inject constructor(accounts: NextcloudAccountRepository) : ViewModel() {
    val accountLabel: StateFlow<String?> = accounts.observeAccount()
        .map { account -> account?.let { accountLabel(it.loginName, it.serverUrl) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** "anna@cloud.example.com". */
internal fun accountLabel(loginName: String, serverUrl: String): String =
    "$loginName@${serverUrl.substringAfter("://")}"
