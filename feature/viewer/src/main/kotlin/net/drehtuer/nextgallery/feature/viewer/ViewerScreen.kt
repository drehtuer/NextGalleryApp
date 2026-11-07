package net.drehtuer.nextgallery.feature.viewer

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.SupportedMimeTypes
import net.drehtuer.nextgallery.core.ui.component.BackButton
import net.drehtuer.nextgallery.core.ui.component.DeleteAction
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.component.ShareAction
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryViewerTheme

private const val CHROME_ALPHA = 0.6f

/** The viewer is always black, whatever the theme, so that the app does not distract from the images. */
@Composable
fun ViewerScreen(
    state: ViewerUiState,
    items: LazyPagingItems<MediaItem>,
    onEvent: (ViewerEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    pageContent: @Composable (MediaItem, onTap: () -> Unit) -> Unit = { item, onTap -> ZoomablePage(item, onTap) },
) {
    NextGalleryViewerTheme {
        SystemBarsEffect(visible = state.chromeVisible)
        val refresh = items.loadState.refresh
        LaunchedEffect(items.itemCount, refresh) {
            if (refresh is LoadState.NotLoading) onEvent(ViewerEvent.ItemCountChanged(items.itemCount))
        }
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
            val initialIndex = state.initialIndex
            if (initialIndex == null || items.itemCount == 0) {
                LoadingIndicator()
            } else {
                ImagePager(initialIndex, items, onEvent, pageContent)
            }
            if (state.chromeVisible) ViewerTopBar(state.current, onEvent, onBack)
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
        state.details?.let { ImageInfoSheet(it, onDismiss = { onEvent(ViewerEvent.HideInfo) }) }
    }
}

@Composable
private fun ImagePager(
    initialIndex: Int,
    items: LazyPagingItems<MediaItem>,
    onEvent: (ViewerEvent) -> Unit,
    pageContent: @Composable (MediaItem, onTap: () -> Unit) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.itemCount }
    // Also reports a new image when the current one was deleted and the next one moved into its place.
    LaunchedEffect(pagerState, items) {
        snapshotFlow {
            items.itemCount.takeIf { it > 0 }?.let { items.peek(pagerState.currentPage.coerceAtMost(it - 1)) }
        }.distinctUntilChanged().collect { onEvent(ViewerEvent.PageChanged(it)) }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = items.itemKey { it.id.value },
    ) { page ->
        val item = items[page]
        if (item == null) {
            LoadingIndicator()
        } else {
            pageContent(item) { onEvent(ViewerEvent.ToggleChrome) }
        }
    }
}

@Composable
private fun ZoomablePage(item: MediaItem, onTap: () -> Unit) {
    ZoomableAsyncImage(
        model = item.contentUri,
        contentDescription = item.displayName,
        modifier = Modifier.fillMaxSize(),
        onClick = { onTap() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerTopBar(current: MediaItem?, onEvent: (ViewerEvent) -> Unit, onBack: () -> Unit) {
    TopAppBar(
        title = {
            current?.let {
                Text(
                    it.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = { BackButton(onClick = onBack) },
        actions = {
            ShareAction(onClick = { onEvent(ViewerEvent.Share) })
            if (current != null && SupportedMimeTypes.isEditable(current.mimeType)) {
                IconButton(onClick = { onEvent(ViewerEvent.Edit) }) {
                    Icon(
                        painter = painterResource(R.drawable.feature_viewer_ic_edit),
                        contentDescription = stringResource(R.string.feature_viewer_edit),
                    )
                }
            }
            DeleteAction(onClick = { onEvent(ViewerEvent.Delete) })
            IconButton(onClick = { onEvent(ViewerEvent.ShowInfo) }) {
                Icon(
                    painter = painterResource(R.drawable.feature_viewer_ic_info),
                    contentDescription = stringResource(R.string.feature_viewer_info),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = CHROME_ALPHA),
        ),
    )
}

/** Hides the system bars together with the viewer's own controls; leaving the viewer restores them. */
@Composable
private fun SystemBarsEffect(visible: Boolean) {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    DisposableEffect(window, view, visible) {
        val controller = WindowCompat.getInsetsController(window, view)
        val lightStatusBars = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatusBars
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
