package net.drehtuer.nextgallery.core.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import net.drehtuer.nextgallery.core.model.UploadAvailability
import net.drehtuer.nextgallery.core.ui.R

private const val UNAVAILABLE_ACTION_ALPHA = 0.38f

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.core_ui_ic_arrow_back), stringResource(R.string.core_ui_back))
    }
}

@Composable
fun ShareAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.core_ui_ic_share), stringResource(R.string.core_ui_share))
    }
}

@Composable
fun DeleteAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.core_ui_ic_delete), stringResource(R.string.core_ui_delete))
    }
}

/**
 * Hands images to the Nextcloud app (D31). Not there while logged out (D35). Without the app it stays tappable but is
 * dimmed: the tap explains what is missing, and it works if the app was installed in the meantime.
 */
@Composable
fun UploadAction(availability: UploadAvailability, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (availability == UploadAvailability.HIDDEN) return
    val appMissing = availability == UploadAvailability.APP_MISSING
    val hint = stringResource(R.string.core_ui_upload_app_missing)
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { if (appMissing) stateDescription = hint },
    ) {
        Icon(
            painterResource(R.drawable.core_ui_ic_upload),
            stringResource(R.string.core_ui_upload),
            Modifier.alpha(if (appMissing) UNAVAILABLE_ACTION_ALPHA else 1f),
        )
    }
}

@Composable
fun SelectAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.core_ui_ic_select), stringResource(R.string.core_ui_select))
    }
}

@Composable
fun SelectAllAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.core_ui_ic_select_all), stringResource(R.string.core_ui_select_all))
    }
}

@Composable
fun SelectionOverviewAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.core_ui_ic_show_selection), stringResource(R.string.core_ui_selection_overview))
    }
}

@Composable
fun SettingsAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.core_ui_ic_settings), stringResource(R.string.core_ui_settings))
    }
}

/** Top bar during a selection. [selectionCount] is the app-wide total, not only the images on this screen (D33). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectionCount: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.core_ui_selected_count, selectionCount, selectionCount)) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(painterResource(R.drawable.core_ui_ic_close), stringResource(R.string.core_ui_clear_selection))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
