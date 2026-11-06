package net.drehtuer.nextgallery.core.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.drehtuer.nextgallery.core.ui.R

/** Shown instead of the app content while the app has no access to photos. */
@Composable
fun MediaAccessRequest(onRequestAccess: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.core_ui_media_access_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.core_ui_media_access_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequestAccess) { Text(stringResource(R.string.core_ui_media_access_allow)) }
        // When the user denied twice, the system no longer shows the dialog; only the settings page helps then.
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.core_ui_media_access_open_settings)) }
    }
}

/** Banner for partial access (only selected photos), with a way to select more (D15). */
@Composable
fun LimitedAccessBanner(onSelectMore: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.core_ui_limited_access_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSelectMore) { Text(stringResource(R.string.core_ui_limited_access_select_more)) }
        }
    }
}
