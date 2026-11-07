package net.drehtuer.nextgallery.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

@Composable
fun TrustedCertificatesScreen(
    certificates: List<CertificateInfo>,
    onRemove: (host: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(stringResource(R.string.feature_settings_certificates), onBack, modifier) { contentModifier ->
        SettingsColumn(contentModifier) {
            if (certificates.isEmpty()) {
                SettingsNote(stringResource(R.string.feature_settings_certificates_empty))
            }
            certificates.forEach { certificate -> CertificateRow(certificate, onRemove) }
        }
    }
}

@Composable
private fun CertificateRow(certificate: CertificateInfo, onRemove: (host: String) -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        Column(Modifier.weight(1f)) {
            Text(certificate.host, style = MaterialTheme.typography.bodyLarge)
            Text(certificate.subject, style = MaterialTheme.typography.bodySmall)
            Text(certificate.sha256Fingerprint, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { onRemove(certificate.host) }) {
            Text(stringResource(R.string.feature_settings_certificate_remove))
        }
    }
}
