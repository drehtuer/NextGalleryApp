package net.drehtuer.nextgallery.feature.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.drehtuer.nextgallery.core.model.edit.AspectRatio
import net.drehtuer.nextgallery.core.model.edit.CropHandle
import net.drehtuer.nextgallery.core.model.edit.CropMath
import net.drehtuer.nextgallery.core.model.edit.NormalizedDistance
import net.drehtuer.nextgallery.core.model.edit.NormalizedRect
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryOverlayColors
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme

internal const val CROP_OVERLAY_TAG = "feature_editor_crop_overlay"
private const val THIRDS = 3

/** The crop frame over the image: the outside darkened, thirds lines, and handles at the corners and edges. */
@Composable
fun CropOverlay(
    rect: NormalizedRect,
    onDrag: (CropHandle, NormalizedDistance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentRect by rememberUpdatedState(rect)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val dimensions = NextGalleryTheme.dimensions
    Canvas(
        modifier.testTag(CROP_OVERLAY_TAG).pointerInput(Unit) {
            val tolerance = dimensions.cropTouchTolerance.toPx()
            fun distance(amount: Offset) = NormalizedDistance(amount.x / size.width, amount.y / size.height)
            awaitEachGesture {
                // The handle is the one under the finger when it touched down, however fast it moves after that.
                val down = awaitFirstDown()
                val handle = CropMath.handleAt(
                    currentRect,
                    down.position.x / size.width,
                    down.position.y / size.height,
                    tolerance / size.width,
                    tolerance / size.height,
                ) ?: return@awaitEachGesture
                val start = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    ?: return@awaitEachGesture
                currentOnDrag(handle, distance(start.position - down.position))
                drag(start.id) { change ->
                    currentOnDrag(handle, distance(change.positionChange()))
                    change.consume()
                }
            }
        },
    ) {
        val frame = Rect(
            rect.left * size.width,
            rect.top * size.height,
            rect.right * size.width,
            rect.bottom * size.height,
        )
        clipRect(frame.left, frame.top, frame.right, frame.bottom, ClipOp.Difference) {
            drawRect(NextGalleryOverlayColors.cropScrim)
        }
        drawThirds(frame, dimensions.cropThirdsWidth.toPx())
        drawRect(
            NextGalleryOverlayColors.cropFrame,
            frame.topLeft,
            frame.size,
            style = Stroke(dimensions.cropFrameWidth.toPx()),
        )
        drawHandles(frame, dimensions.cropHandleWidth.toPx(), dimensions.cropHandleLength.toPx())
    }
}

private fun DrawScope.drawThirds(frame: Rect, width: Float) {
    for (line in 1 until THIRDS) {
        val x = frame.left + frame.width * line / THIRDS
        val y = frame.top + frame.height * line / THIRDS
        drawLine(NextGalleryOverlayColors.cropFrame, Offset(x, frame.top), Offset(x, frame.bottom), width)
        drawLine(NextGalleryOverlayColors.cropFrame, Offset(frame.left, y), Offset(frame.right, y), width)
    }
}

private fun DrawScope.drawHandles(frame: Rect, thickness: Float, length: Float) {
    fun bar(centerX: Float, centerY: Float, width: Float, height: Float) = drawRect(
        NextGalleryOverlayColors.cropFrame,
        Offset(centerX - width / 2, centerY - height / 2),
        Size(width, height),
    )
    listOf(frame.left, frame.center.x, frame.right).forEach { x ->
        listOf(frame.top, frame.bottom).forEach { y -> bar(x, y, length, thickness) }
    }
    listOf(frame.top, frame.center.y, frame.bottom).forEach { y ->
        listOf(frame.left, frame.right).forEach { x -> bar(x, y, thickness, length) }
    }
}

@Composable
fun AspectChips(
    selected: AspectRatio,
    enabled: Boolean,
    onSelect: (AspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NextGalleryTheme.dimensions.spacingSmall
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        AspectRatio.entries.forEach { aspect ->
            FilterChip(
                selected = aspect == selected,
                onClick = { onSelect(aspect) },
                label = { Text(stringResource(aspect.label)) },
                enabled = enabled,
            )
        }
    }
}

@get:StringRes
private val AspectRatio.label: Int
    get() = when (this) {
        AspectRatio.FREE -> R.string.feature_editor_aspect_free
        AspectRatio.ORIGINAL -> R.string.feature_editor_aspect_original
        AspectRatio.SQUARE -> R.string.feature_editor_aspect_square
        AspectRatio.R4_3 -> R.string.feature_editor_aspect_4_3
        AspectRatio.R16_9 -> R.string.feature_editor_aspect_16_9
    }
