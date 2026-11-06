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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.ui.R

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

@Composable
fun SelectionOverviewAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.core_ui_ic_show_selection), stringResource(R.string.core_ui_selection_overview))
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
