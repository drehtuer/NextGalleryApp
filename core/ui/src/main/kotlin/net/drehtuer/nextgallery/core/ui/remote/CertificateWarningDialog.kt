package net.drehtuer.nextgallery.core.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.model.CertificateInfo
import net.drehtuer.nextgallery.core.ui.R
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The server's certificate needs a decision (D32). Accepting is deliberately the less prominent button, in the error
 * colour; dismissing the dialog in any other way rejects the certificate.
 */
@Composable
fun CertificateWarningDialog(
    info: CertificateInfo,
    onTrust: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changed = info.changedFromPinned
    AlertDialog(
        onDismissRequest = onReject,
        modifier = modifier,
        title = {
            Text(
                stringResource(
                    if (changed) R.string.core_ui_certificate_changed_title else R.string.core_ui_certificate_title,
                ),
            )
        },
        text = {
            val dimensions = NextGalleryTheme.dimensions
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
            ) {
                if (changed) CertificateChangedDetails(info) else CertificateDetails(info)
            }
        },
        confirmButton = {
            Button(onClick = onReject) {
                Text(stringResource(if (changed) R.string.core_ui_certificate_disconnect else R.string.core_ui_cancel))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onTrust,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(
                    stringResource(
                        if (changed) R.string.core_ui_certificate_trust_new else R.string.core_ui_certificate_trust,
                    ),
                )
            }
        },
    )
}

@Composable
private fun CertificateDetails(info: CertificateInfo) {
    val validUntil = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(info.notAfterMillis))
    DetailRow(stringResource(R.string.core_ui_certificate_issued_to), info.subject)
    DetailRow(stringResource(R.string.core_ui_certificate_issued_by), info.issuer)
    DetailRow(stringResource(R.string.core_ui_certificate_valid_until), validUntil)
    DetailRow(stringResource(R.string.core_ui_certificate_sha256), info.sha256Fingerprint)
}

@Composable
private fun CertificateChangedDetails(info: CertificateInfo) {
    Text(stringResource(R.string.core_ui_certificate_changed_message, info.host))
    DetailRow(stringResource(R.string.core_ui_certificate_accepted), info.pinnedFingerprint.orEmpty())
    DetailRow(stringResource(R.string.core_ui_certificate_new), info.sha256Fingerprint)
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
