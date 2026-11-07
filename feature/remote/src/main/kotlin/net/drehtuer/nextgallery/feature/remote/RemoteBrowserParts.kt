package net.drehtuer.nextgallery.feature.remote

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.ui.remote.RemoteFolderRow
import net.drehtuer.nextgallery.core.ui.remote.RemoteThumbnailCell
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.thumbnail.LARGE_THUMBNAIL_PX

/** Shown instead of the browser while logged out (D35): there is no access to the server at all. */
@Composable
internal fun LoginRequired(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dimensions = NextGalleryTheme.dimensions
    Column(
        modifier.fillMaxSize().padding(dimensions.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(dimensions.iconTileSize)
                .background(colors.primaryContainer, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.feature_remote_ic_cloud),
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconTileIconSize),
                tint = colors.onPrimaryContainer,
            )
        }
        Text(stringResource(R.string.feature_remote_login_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.feature_remote_login_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = colors.onSurfaceVariant,
        )
        Button(onClick = onSignIn) { Text(stringResource(R.string.feature_remote_sign_in)) }
    }
}

@Composable
internal fun OfflineBanner() {
    val colors = MaterialTheme.colorScheme
    val dimensions = NextGalleryTheme.dimensions
    Text(
        stringResource(R.string.feature_remote_offline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall)
            .background(colors.secondaryContainer, MaterialTheme.shapes.medium)
            .padding(dimensions.spacingMedium),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSecondaryContainer,
    )
}

@Composable
internal fun SessionEndedBanner(onSignIn: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dimensions = NextGalleryTheme.dimensions
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall)
            .background(colors.errorContainer, MaterialTheme.shapes.medium)
            .padding(dimensions.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.feature_remote_session_ended),
            modifier = Modifier.weight(1f).padding(dimensions.spacingSmall),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onErrorContainer,
        )
        TextButton(onClick = onSignIn) {
            Text(stringResource(R.string.feature_remote_sign_in), color = colors.onErrorContainer)
        }
    }
}

/** The folder rows, then the images in a grid with the column count of the Appearance setting. */
@Composable
internal fun RemoteGrid(state: RemoteBrowserUiState, onEvent: (RemoteBrowserEvent) -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    LazyVerticalGrid(
        columns = GridCells.Fixed(dimensions.gridColumns),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(dimensions.gridGap),
        verticalArrangement = Arrangement.spacedBy(dimensions.gridGap),
    ) {
        items(state.folders, key = { it.path }, span = { GridItemSpan(maxLineSpan) }) { folder ->
            RemoteFolderRow(
                name = folder.name,
                detail = folder.childCount?.let { count ->
                    pluralStringResource(R.plurals.feature_remote_folder_count, count, count)
                },
                onClick = { onEvent(BrowseEvent.OpenFolder(folder.path)) },
            )
        }
        items(state.images, key = { it.path }) { image -> RemoteGridImage(image, state, onEvent) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RemoteGridImage(item: RemoteItem, state: RemoteBrowserUiState, onEvent: (RemoteBrowserEvent) -> Unit) {
    var pressPosition by remember { mutableStateOf(Offset.Zero) }
    var cellHeight by remember { mutableStateOf(0) }
    Box(
        Modifier
            .onSizeChanged { cellHeight = it.height }
            .pointerInput(Unit) {
                awaitEachGesture {
                    pressPosition = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position
                }
            }
            .combinedClickable(
                onLongClickLabel = stringResource(R.string.feature_remote_menu_label),
                onClick = { onEvent(SelectionEvent.ItemClicked(item)) },
                onLongClick = { onEvent(SelectionEvent.ItemLongPressed(item)) },
            ),
    ) {
        RemoteThumbnailCell(
            item = item,
            url = state.urls?.preview(item, LARGE_THUMBNAIL_PX),
            sizePx = LARGE_THUMBNAIL_PX,
            selected = item.path in state.selection,
            selecting = state.selecting,
            offline = state.isOffline,
        )
        state.menu?.takeIf { it.anchorPath == item.path }?.let { menu ->
            val density = LocalDensity.current
            RemoteContextMenu(
                menu = menu,
                // A menu's offset counts from the bottom left of its anchor.
                offset = with(density) { DpOffset(pressPosition.x.toDp(), (pressPosition.y - cellHeight).toDp()) },
                onEvent = onEvent,
            )
        }
    }
}

/** D47: at the touch point, with a header, for one image or the whole selection. No Properties: the viewer has them. */
@Composable
private fun RemoteContextMenu(menu: RemoteMenuState, offset: DpOffset, onEvent: (RemoteBrowserEvent) -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    DropdownMenu(
        expanded = true,
        onDismissRequest = { onEvent(SelectionEvent.MenuDismissed) },
        modifier = Modifier.width(dimensions.contextMenuWidth),
        offset = offset,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            menu.items.singleOrNull()?.name
                ?: pluralStringResource(R.plurals.feature_remote_menu_selected, menu.items.size, menu.items.size),
            modifier = Modifier.padding(dimensions.spacingMedium),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.feature_remote_menu_move)) },
            onClick = { onEvent(SelectionEvent.MenuMove) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.feature_remote_menu_share)) },
            onClick = { onEvent(SelectionEvent.MenuShare) },
        )
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.feature_remote_menu_delete), color = MaterialTheme.colorScheme.error)
            },
            onClick = { onEvent(SelectionEvent.MenuDelete) },
        )
    }
}
