package net.drehtuer.nextgallery.feature.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteDownloadProgress
import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemoteItem
import net.drehtuer.nextgallery.core.ui.component.BackButton
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.remote.remoteImageCacheKey
import net.drehtuer.nextgallery.core.ui.remote.remoteImageLoader
import net.drehtuer.nextgallery.core.ui.remote.remoteMessage
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryViewerTheme

private const val CHROME_ALPHA = 0.6f
private const val BYTES_PER_MB = 1024f * 1024f
private const val VIEWER_PREVIEW_PX = 2048

/** Always black, like the local viewer. The server's preview shows first, then the original replaces it. */
@Composable
fun RemoteViewerScreen(
    state: RemoteViewerUiState,
    onEvent: (RemoteViewerEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    pageContent: @Composable (RemoteItem, onTap: () -> Unit) -> Unit = { item, onTap ->
        RemoteZoomablePage(item, state.urls, onTap)
    },
) {
    NextGalleryViewerTheme {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
            val initialIndex = state.initialIndex
            val error = state.error
            when {
                initialIndex != null -> RemotePager(state, initialIndex, onEvent, pageContent)
                error != null -> EmptyState(title = stringResource(error.remoteMessage()))
                else -> LoadingIndicator()
            }
            if (state.chromeVisible) RemoteViewerTopBar(state.current, onEvent, onBack)
            state.progress?.let { progress ->
                RemoteDownloadBar(progress, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
            }
        }
        if (state.showInfo) {
            state.current?.let { RemoteInfoSheet(it, onDismiss = { onEvent(RemoteViewerEvent.HideInfo) }) }
        }
    }
}

@Composable
private fun RemotePager(
    state: RemoteViewerUiState,
    initialIndex: Int,
    onEvent: (RemoteViewerEvent) -> Unit,
    pageContent: @Composable (RemoteItem, onTap: () -> Unit) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { state.images.size }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onEvent(RemoteViewerEvent.PageChanged(it)) }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // Originals are only loaded for the image on screen.
        beyondViewportPageCount = 0,
        key = { index -> state.images[index].path },
    ) { page ->
        pageContent(state.images[page]) { onEvent(RemoteViewerEvent.ToggleChrome) }
    }
}

@Composable
private fun RemoteZoomablePage(item: RemoteItem, urls: RemoteImageUrls?, onTap: () -> Unit) {
    if (urls == null) return
    val context = LocalContext.current
    val loader = remoteImageLoader()
    Box(Modifier.fillMaxSize()) {
        urls.preview(item, VIEWER_PREVIEW_PX)?.let { preview ->
            val key = remoteImageCacheKey(item, VIEWER_PREVIEW_PX)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(preview)
                    .size(VIEWER_PREVIEW_PX)
                    .memoryCacheKey(key)
                    .diskCacheKey(key)
                    .build(),
                imageLoader = loader,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        val key = remoteImageCacheKey(item, null)
        ZoomableAsyncImage(
            model = ImageRequest.Builder(context)
                .data(urls.original(item))
                .memoryCacheKey(key)
                .diskCacheKey(key)
                .build(),
            imageLoader = loader,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            onClick = { onTap() },
        )
    }
}

@Composable
private fun RemoteDownloadBar(progress: RemoteDownloadProgress, modifier: Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    Column(
        modifier.fillMaxWidth().padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        if (progress.totalBytes > 0) {
            LinearProgressIndicator(
                progress = { progress.bytesRead.toFloat() / progress.totalBytes },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Text(
            stringResource(
                R.string.feature_remote_loading_full,
                progress.bytesRead / BYTES_PER_MB,
                progress.totalBytes.coerceAtLeast(0) / BYTES_PER_MB,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteViewerTopBar(current: RemoteItem?, onEvent: (RemoteViewerEvent) -> Unit, onBack: () -> Unit) {
    TopAppBar(
        title = {
            current?.let {
                Text(
                    it.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = { BackButton(onClick = onBack) },
        actions = {
            IconButton(onClick = { onEvent(RemoteViewerEvent.ShowInfo) }) {
                Icon(
                    painterResource(R.drawable.feature_remote_ic_info),
                    contentDescription = stringResource(R.string.feature_remote_details),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = CHROME_ALPHA),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteInfoSheet(item: RemoteItem, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { RemoteInfoContent(item) }
}

/** Values the server did not send are left out. */
@Composable
internal fun RemoteInfoContent(item: RemoteItem, modifier: Modifier = Modifier) {
    val dimensions = NextGalleryTheme.dimensions
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        InfoRow(R.string.feature_remote_info_name, item.name)
        InfoRow(R.string.feature_remote_info_date, formatDate(item).takeIf { item.lastModifiedMillis > 0 })
        InfoRow(R.string.feature_remote_info_size, formatSize(item))
        InfoRow(R.string.feature_remote_info_type, item.mime)
        InfoRow(R.string.feature_remote_info_folder, item.parentPath)
    }
}

@Composable
private fun InfoRow(label: Int, value: String?) {
    if (value == null) return
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
