package net.drehtuer.nextgallery.feature.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import net.drehtuer.nextgallery.core.model.edit.NormalizedPoint
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryOverlayColors
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import kotlin.math.max
import kotlin.math.roundToInt

internal const val PEN_CANVAS_TAG = "feature_editor_pen_canvas"

private const val MIN_PEN_WIDTH = 1
private const val MAX_PEN_WIDTH = 64

private class DrawnStroke(val points: List<NormalizedPoint>, val color: Color, val widthFraction: Float)

/**
 * Takes strokes with the finger. The strokes stay on this overlay until a new [preview] contains them, so that they
 * do not flicker while it renders.
 */
@Composable
fun PenCanvas(
    preview: PreviewImage,
    colorArgb: Int,
    widthFraction: Float,
    onStroke: (List<NormalizedPoint>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = remember { mutableStateListOf<NormalizedPoint>() }
    val finished = remember(preview) { mutableStateListOf<DrawnStroke>() }
    val currentColor by rememberUpdatedState(Color(colorArgb))
    val currentWidth by rememberUpdatedState(widthFraction)
    val currentOnStroke by rememberUpdatedState(onStroke)
    val currentFinished by rememberUpdatedState(finished)
    Canvas(
        modifier.testTag(PEN_CANVAS_TAG).pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                current.clear()
                current += NormalizedPoint.clamped(down.position.x / size.width, down.position.y / size.height)
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.first()
                    if (change.pressed) {
                        change.consume()
                        current +=
                            NormalizedPoint.clamped(change.position.x / size.width, change.position.y / size.height)
                    }
                } while (event.changes.any { it.pressed })
                val points = current.toList()
                currentFinished += DrawnStroke(points, currentColor, currentWidth)
                current.clear()
                currentOnStroke(points)
            }
        },
    ) {
        finished.forEach { drawStroke(it.points, it.color, it.widthFraction) }
        drawStroke(current, Color(colorArgb), widthFraction)
    }
}

private fun DrawScope.drawStroke(points: List<NormalizedPoint>, color: Color, widthFraction: Float) {
    val first = points.firstOrNull() ?: return
    val path = Path().apply {
        moveTo(first.x * size.width, first.y * size.height)
        lineTo(first.x * size.width, first.y * size.height)
        points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
    }
    drawPath(
        path,
        color,
        style = Stroke(
            width = widthFraction * max(size.width, size.height),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

@Composable
fun PenOptions(
    colorArgb: Int,
    width: Int,
    onColor: (Int) -> Unit,
    onWidth: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NextGalleryTheme.dimensions.spacingSmall
    Column(modifier.fillMaxWidth().padding(horizontal = NextGalleryTheme.dimensions.spacingMedium)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
            NextGalleryOverlayColors.penColors.zip(PEN_COLOR_NAMES).forEach { (color, name) ->
                PenSwatch(color, stringResource(name), color.toArgb() == colorArgb) { onColor(color.toArgb()) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = width.toFloat(),
                onValueChange = { onWidth(it.roundToInt()) },
                valueRange = MIN_PEN_WIDTH.toFloat()..MAX_PEN_WIDTH.toFloat(),
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.feature_editor_pen_width, width),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = spacing),
            )
        }
    }
}

@Composable
private fun PenSwatch(color: Color, name: String, selected: Boolean, onClick: () -> Unit) {
    val dimensions = NextGalleryTheme.dimensions
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val ringWidth = if (selected) dimensions.selectionBorderWidth else dimensions.gridGap
    Box(
        Modifier
            .size(dimensions.penSwatchSize)
            .background(color, CircleShape)
            .border(ringWidth, ring, CircleShape)
            .selectable(selected = selected, onClick = onClick)
            .semantics { contentDescription = name },
    )
}

/** In the order of `NextGalleryOverlayColors.penColors`. */
@StringRes
private val PEN_COLOR_NAMES = listOf(
    R.string.feature_editor_color_white,
    R.string.feature_editor_color_black,
    R.string.feature_editor_color_coral,
    R.string.feature_editor_color_orange,
    R.string.feature_editor_color_green,
    R.string.feature_editor_color_blue,
)
