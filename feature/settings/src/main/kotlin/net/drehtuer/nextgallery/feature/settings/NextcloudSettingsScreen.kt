package net.drehtuer.nextgallery.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import net.drehtuer.nextgallery.core.model.RemotePaths
import net.drehtuer.nextgallery.core.ui.remote.Breadcrumbs
import net.drehtuer.nextgallery.core.ui.remote.CertificateWarningDialog
import net.drehtuer.nextgallery.core.ui.remote.RemoteFolderRow
import net.drehtuer.nextgallery.core.ui.remote.remoteMessage
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

/** The account: signing in through the browser (the app never sees the password), or the account and logging out. */
@Composable
fun NextcloudSettingsScreen(
    state: NextcloudSettingsUiState,
    onEvent: (NextcloudSettingsEvent) -> Unit,
    onBack: () -> Unit,
    onOpenCertificates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(stringResource(R.string.feature_settings_nextcloud), onBack, modifier) { contentModifier ->
        SettingsColumn(contentModifier) {
            if (state.showForm) SignInForm(state, onEvent) else AccountDetails(state, onEvent)
            SettingsRow(
                title = stringResource(R.string.feature_settings_certificates),
                detail = stringResource(R.string.feature_settings_certificates_detail),
                icon = R.drawable.feature_settings_ic_lock,
                onClick = onOpenCertificates,
            )
        }
    }
    state.certificate?.let { info ->
        CertificateWarningDialog(
            info = info,
            onTrust = { onEvent(NextcloudSettingsEvent.TrustCertificate) },
            onReject = { onEvent(NextcloudSettingsEvent.RejectCertificate) },
        )
    }
    if (state.showLocalNetworkExplanation) LocalNetworkExplanationDialog(onEvent)
    state.rootPicker?.let { RemoteRootPickerSheet(it, onEvent) }
}

@Composable
private fun ColumnScope.SignInForm(state: NextcloudSettingsUiState, onEvent: (NextcloudSettingsEvent) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dimensions = NextGalleryTheme.dimensions
    Box(
        Modifier
            .align(Alignment.CenterHorizontally)
            .size(dimensions.iconTileSize)
            .background(colors.primaryContainer, MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.feature_settings_ic_cloud),
            contentDescription = null,
            modifier = Modifier.size(dimensions.iconTileIconSize),
            tint = colors.onPrimaryContainer,
        )
    }
    Text(
        stringResource(R.string.feature_settings_nextcloud_title),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    Text(
        stringResource(R.string.feature_settings_nextcloud_message),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = colors.onSurfaceVariant,
    )
    OutlinedTextField(
        value = state.serverAddress,
        onValueChange = { onEvent(NextcloudSettingsEvent.ServerAddressChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.feature_settings_server_address)) },
        enabled = !state.isSigningIn,
        singleLine = true,
        isError = state.error != null,
        supportingText = state.error?.let { error -> { Text(stringResource(error.remoteMessage())) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
    if (state.isSigningIn) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(R.string.feature_settings_waiting_for_browser), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { onEvent(NextcloudSettingsEvent.CancelSignIn) }) {
            Text(stringResource(CoreUiR.string.core_ui_cancel))
        }
    } else {
        Button(
            onClick = { onEvent(NextcloudSettingsEvent.SignIn) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.serverAddress.isNotBlank(),
        ) {
            Text(stringResource(R.string.feature_settings_sign_in))
        }
    }
}

@Composable
private fun AccountDetails(state: NextcloudSettingsUiState, onEvent: (NextcloudSettingsEvent) -> Unit) {
    SettingsRow(
        title = state.accountLabel.orEmpty(),
        detail = stringResource(R.string.feature_settings_connected),
        icon = R.drawable.feature_settings_ic_cloud,
    )
    SettingsRow(
        title = stringResource(R.string.feature_settings_remote_root),
        detail = state.remoteRoot,
        icon = R.drawable.feature_settings_ic_folder,
        onClick = { onEvent(NextcloudSettingsEvent.OpenRootPicker) },
    )
    TextButton(onClick = { onEvent(NextcloudSettingsEvent.LogOut) }) {
        Text(stringResource(R.string.feature_settings_log_out), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun LocalNetworkExplanationDialog(onEvent: (NextcloudSettingsEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(NextcloudSettingsEvent.LocalNetworkExplanationDismissed) },
        title = { Text(stringResource(R.string.feature_settings_local_network_title)) },
        text = { Text(stringResource(R.string.feature_settings_local_network_message)) },
        confirmButton = {
            Button(onClick = { onEvent(NextcloudSettingsEvent.LocalNetworkExplanationAccepted) }) {
                Text(stringResource(R.string.feature_settings_local_network_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(NextcloudSettingsEvent.LocalNetworkExplanationDismissed) }) {
                Text(stringResource(CoreUiR.string.core_ui_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteRootPickerSheet(state: RemoteRootPickerState, onEvent: (NextcloudSettingsEvent) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onEvent(NextcloudSettingsEvent.RootPickerDismissed) }) {
        RemoteRootPickerContent(state, onEvent)
    }
}

@Composable
internal fun RemoteRootPickerContent(
    state: RemoteRootPickerState,
    onEvent: (NextcloudSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = NextGalleryTheme.dimensions
    Column(modifier, verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
        Breadcrumbs(state.path, onOpen = { onEvent(NextcloudSettingsEvent.RootPickerOpened(it)) })
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { SettingsNote(stringResource(it.remoteMessage())) }
        state.folders.forEach { folder ->
            RemoteFolderRow(folder.name, onClick = { onEvent(NextcloudSettingsEvent.RootPickerOpened(folder.path)) })
        }
        Button(
            onClick = { onEvent(NextcloudSettingsEvent.RootPickerConfirmed) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            val name = RemotePaths.name(state.path).ifEmpty { stringResource(CoreUiR.string.core_ui_remote_root) }
            Text(stringResource(R.string.feature_settings_remote_root_use, name))
        }
    }
}
