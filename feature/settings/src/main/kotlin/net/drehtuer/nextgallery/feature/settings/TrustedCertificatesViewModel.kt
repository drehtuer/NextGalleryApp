package net.drehtuer.nextgallery.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.drehtuer.nextgallery.core.data.nextcloud.TrustedCertificateRepository
import net.drehtuer.nextgallery.core.model.CertificateInfo
import javax.inject.Inject

/** The certificates the user accepted (D32). Removing one means the warning appears again at the next connection. */
@HiltViewModel
class TrustedCertificatesViewModel @Inject constructor(private val certificates: TrustedCertificateRepository) :
    ViewModel() {
    val state: StateFlow<List<CertificateInfo>> = certificates.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun remove(host: String) {
        viewModelScope.launch { certificates.remove(host) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
