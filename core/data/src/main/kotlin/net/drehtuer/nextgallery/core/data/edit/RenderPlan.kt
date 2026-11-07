package net.drehtuer.nextgallery.core.data.edit

import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.Matrix3
import net.drehtuer.nextgallery.core.model.edit.PlanePoint
import kotlin.math.max
import kotlin.math.roundToInt

/** A pen stroke on the output image, in pixels. */
data class RenderStroke(val points: List<PlanePoint>, val colorArgb: Int, val widthPixels: Double)

/**
 * All operations flattened into one drawing: [sourceMatrix] maps the pixels of the source to the output image,
 * which by its size also clips everything a crop removed; the [strokes] are drawn over it, in order. So an edit only
 * ever needs the source and the output bitmap (D30), and a later scale does not blur what came before.
 */
data class RenderPlan(
    val sourceSize: ImageSize,
    val outputSize: ImageSize,
    val sourceMatrix: Matrix3,
    val strokes: List<RenderStroke>,
) {
    /** The same picture with the longer side at most [maxSide], for the preview. */
    fun fittedTo(maxSide: Int): RenderPlan {
        if (outputSize.longSide <= maxSide) return this
        val factor = maxSide.toDouble() / outputSize.longSide
        val fitted = ImageSize(
            max(1, (outputSize.width * factor).roundToInt()),
            max(1, (outputSize.height * factor).roundToInt()),
        )
        val scale = Matrix3.scale(
            fitted.width.toDouble() / outputSize.width,
            fitted.height.toDouble() / outputSize.height,
        )
        return copy(
            outputSize = fitted,
            sourceMatrix = sourceMatrix.then(scale),
            strokes = strokes.map { stroke ->
                stroke.copy(
                    points = stroke.points.map { scale.map(it.x, it.y) },
                    widthPixels = stroke.widthPixels * scale.averageScale(),
                )
            },
        )
    }
}
