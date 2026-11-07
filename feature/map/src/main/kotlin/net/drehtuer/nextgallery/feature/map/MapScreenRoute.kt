package net.drehtuer.nextgallery.feature.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.serialization.Serializable
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.ui.platform.rememberShareHandler
import net.drehtuer.nextgallery.core.ui.platform.rememberTrashEffectHandler
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryOverlayColors
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

@Serializable
data object MapScreenRoute

private const val HALO_ALPHA = 0.2f

fun NavGraphBuilder.mapScreen(
    onOpenViewer: (id: MediaId, bounds: GeoBounds) -> Unit,
    onOpenSelectionOverview: () -> Unit,
) {
    composable<MapScreenRoute> {
        val viewModel = hiltViewModel<MapViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val handleShare = rememberShareHandler(snackbarHostState)
        val renderer = rememberOsmdroidRenderer()
        val locationUnavailable = stringResource(R.string.feature_map_location_unavailable)
        val handleTrashEffect = rememberTrashEffectHandler(
            snackbarHostState = snackbarHostState,
            onConfirmationResult = { viewModel.onEvent(MapSelectionEvent.ConfirmationResult(it)) },
            onUndo = { viewModel.onEvent(MapSelectionEvent.UndoDelete) },
        )
        val photoLocationPrompt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.onEvent(MapEvent.PermissionsChanged)
        }
        val currentLocationPrompt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.onEvent(MapEvent.CurrentLocationPromptClosed)
        }
        LifecycleResumeEffect(viewModel) {
            viewModel.onEvent(MapEvent.PermissionsChanged)
            onPauseOrDispose {}
        }
        LaunchedEffect(viewModel, renderer) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is MapEffect.MoveCamera -> renderer.setCamera(effect.camera)

                    MapEffect.RequestPhotoLocationPermission ->
                        photoLocationPrompt.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)

                    MapEffect.RequestCurrentLocationPermission ->
                        currentLocationPrompt.launch(Manifest.permission.ACCESS_COARSE_LOCATION)

                    MapEffect.LocationUnavailable -> snackbarHostState.showSnackbar(locationUnavailable)

                    is MapEffect.OpenViewer -> onOpenViewer(effect.id, effect.bounds)

                    is MapEffect.Share -> handleShare(effect.spec)

                    is MapEffect.Trash -> handleTrashEffect(effect.effect)
                }
            }
        }
        MapRendererBinding(renderer, state, viewModel::onEvent)
        MapScreen(
            state = state,
            sheetItems = viewModel.sheetItems.collectAsLazyPagingItems(),
            onEvent = viewModel::onEvent,
            onOpenSelectionOverview = onOpenSelectionOverview,
            map = { modifier -> key(renderer) { AndroidView(factory = { renderer.view }, modifier = modifier) } },
            snackbarHostState = snackbarHostState,
        )
    }
}

/** One map view per visit to the screen; it follows the screen's lifecycle, as osmdroid requires. */
@Composable
private fun rememberOsmdroidRenderer(): OsmdroidMapRenderer {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val style = markerStyle(LocalDensity.current, colors.primary, colors.onPrimary)
    val land = NextGalleryTheme.mapColors.land.toArgb()
    val water = NextGalleryTheme.mapColors.water.toArgb()
    val renderer = remember(style) {
        OsmdroidMapRenderer(context, style).apply {
            view.overlayManager.tilesOverlay.loadingBackgroundColor = land
            view.overlayManager.tilesOverlay.loadingLineColor = water
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(renderer, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> renderer.view.onResume()
                Lifecycle.Event.ON_PAUSE -> renderer.view.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            renderer.view.onDetach()
        }
    }
    return renderer
}

@Composable
private fun markerStyle(density: Density, accent: Color, onAccent: Color): MarkerStyle {
    val dimensions = NextGalleryTheme.dimensions
    return with(density) {
        MarkerStyle(
            markerSizePx = dimensions.clusterMarkerSize.roundToPx(),
            borderPx = dimensions.clusterMarkerBorder.toPx(),
            pillHeightPx = dimensions.clusterPillHeight.toPx(),
            pillTextSizePx = MaterialTheme.typography.labelMedium.fontSize.toPx(),
            pillPaddingPx = dimensions.spacingSmall.toPx(),
            dotSizePx = dimensions.locationDotSize.toPx(),
            dotRingPx = dimensions.locationDotRing.toPx(),
            haloSizePx = dimensions.locationHaloSize.roundToPx(),
            accent = accent.toArgb(),
            onAccent = onAccent.toArgb(),
            placeholder = MaterialTheme.colorScheme.surfaceVariant.toArgb(),
            ring = NextGalleryOverlayColors.badgeOn.toArgb(),
            halo = accent.copy(alpha = HALO_ALPHA).toArgb(),
        )
    }
}
