package net.drehtuer.nextgallery.feature.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import net.drehtuer.nextgallery.core.data.edit.SaveMode
import net.drehtuer.nextgallery.core.ui.component.EmptyState
import net.drehtuer.nextgallery.core.ui.component.LoadingIndicator
import net.drehtuer.nextgallery.core.ui.component.PreviewImageView
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryViewerTheme
import net.drehtuer.nextgallery.core.ui.R as CoreUiR

/** The editor is always black, like the viewer, so that the colours of the image can be judged (DESIGN.md). */
@Composable
fun EditorScreen(state: EditorUiState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    NextGalleryViewerTheme {
        LightStatusBarIconsEffect()
        BackHandler { onEvent(EditorEvent.Close) }
        Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
            EditorTopBar(state, onEvent)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ImageArea(state, onEvent)
                if (state.isSaving) LoadingIndicator()
            }
            if (state.preview != null) {
                ToolOptions(state, onEvent)
                ToolBar(state, onEvent)
            }
        }
        EditorDialogs(state, onEvent)
    }
}

@Composable
private fun ImageArea(state: EditorUiState, onEvent: (EditorEvent) -> Unit) {
    val preview = state.preview
    when {
        state.loadFailed -> EmptyState(stringResource(R.string.feature_editor_load_failed))

        preview == null -> if (state.isLoading) LoadingIndicator()

        else -> Box(Modifier.aspectRatio(preview.width.toFloat() / preview.height)) {
            PreviewImageView(preview, state.displayName, Modifier.fillMaxSize())
            when (state.tool) {
                EditorTool.CROP -> CropOverlay(
                    rect = state.cropRect,
                    onDrag = { handle, distance -> onEvent(EditorEvent.CropDragged(handle, distance)) },
                    modifier = Modifier.fillMaxSize(),
                )

                EditorTool.DRAW -> PenCanvas(
                    preview = preview,
                    colorArgb = state.penColor,
                    widthFraction = state.penWidthFraction,
                    onStroke = { onEvent(EditorEvent.StrokeDrawn(it)) },
                    modifier = Modifier.fillMaxSize(),
                )

                null -> Unit
            }
        }
    }
}

@Composable
private fun EditorTopBar(state: EditorUiState, onEvent: (EditorEvent) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BarIcon(CoreUiR.drawable.core_ui_ic_close, R.string.feature_editor_close, !state.isSaving) {
            onEvent(EditorEvent.Close)
        }
        BarIcon(R.drawable.feature_editor_ic_undo, R.string.feature_editor_undo, state.canUndo && !state.isSaving) {
            onEvent(EditorEvent.Undo)
        }
        BarIcon(R.drawable.feature_editor_ic_redo, R.string.feature_editor_redo, state.canRedo && !state.isSaving) {
            onEvent(EditorEvent.Redo)
        }
        Box(Modifier.weight(1f))
        SaveButton(enabled = state.hasChanges && !state.isSaving, onEvent = onEvent)
    }
}

@Composable
private fun BarIcon(@DrawableRes icon: Int, @StringRes label: Int, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painterResource(icon), stringResource(label))
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onEvent: (EditorEvent) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(Modifier.padding(end = NextGalleryTheme.dimensions.spacingSmall)) {
        Button(onClick = { menuOpen = true }, enabled = enabled) {
            Text(stringResource(R.string.feature_editor_save))
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            SaveMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(mode.label)) },
                    onClick = {
                        menuOpen = false
                        onEvent(EditorEvent.Save(mode))
                    },
                )
            }
        }
    }
}

private val SaveMode.label: Int
    get() = when (this) {
        SaveMode.NEW_COPY -> R.string.feature_editor_save_copy
        SaveMode.OVERWRITE -> R.string.feature_editor_save_overwrite
    }

@Composable
private fun ToolOptions(state: EditorUiState, onEvent: (EditorEvent) -> Unit) {
    when (state.tool) {
        EditorTool.CROP -> AspectChips(
            selected = state.aspect,
            enabled = !state.isSaving,
            onSelect = { onEvent(EditorEvent.AspectChanged(it)) },
        )

        EditorTool.DRAW -> PenOptions(
            colorArgb = state.penColor,
            width = state.penWidth,
            onColor = { onEvent(EditorEvent.PenColorChanged(it)) },
            onWidth = { onEvent(EditorEvent.PenWidthChanged(it)) },
        )

        null -> Unit
    }
}

@Composable
private fun ToolBar(state: EditorUiState, onEvent: (EditorEvent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = NextGalleryTheme.dimensions.spacingSmall),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToolButton(R.drawable.feature_editor_ic_rotate, R.string.feature_editor_tool_rotate, false) {
            onEvent(EditorEvent.Rotate)
        }
        ToolButton(
            R.drawable.feature_editor_ic_crop,
            R.string.feature_editor_tool_crop,
            state.tool == EditorTool.CROP,
        ) {
            onEvent(EditorEvent.SelectTool(EditorTool.CROP))
        }
        ToolButton(R.drawable.feature_editor_ic_scale, R.string.feature_editor_tool_scale, false) {
            onEvent(EditorEvent.ShowScaleDialog)
        }
        ToolButton(
            R.drawable.feature_editor_ic_draw,
            R.string.feature_editor_tool_draw,
            state.tool == EditorTool.DRAW,
        ) {
            onEvent(EditorEvent.SelectTool(EditorTool.DRAW))
        }
    }
}

@Composable
private fun ToolButton(@DrawableRes icon: Int, @StringRes label: Int, active: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.selectable(selected = active, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val dimensions = NextGalleryTheme.dimensions
        val pill = if (active) Modifier.background(colors.primaryContainer, MaterialTheme.shapes.large) else Modifier
        Box(
            Modifier.size(dimensions.editorToolPillWidth, dimensions.editorToolPillHeight).then(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (active) colors.onPrimaryContainer else colors.onSurface,
            )
        }
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
    }
}

/** Black behind the status bar needs light icons, also in the light theme; leaving the editor restores them. */
@Composable
private fun LightStatusBarIconsEffect() {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val lightStatusBars = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = lightStatusBars }
    }
}
