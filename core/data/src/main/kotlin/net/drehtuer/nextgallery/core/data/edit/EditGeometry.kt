package net.drehtuer.nextgallery.core.data.edit

import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.GeometryMapper
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.Matrix3
import net.drehtuer.nextgallery.core.model.edit.NormalizedRect
import kotlin.math.roundToInt

/**
 * Turns operations into a [RenderPlan] for a given source size. The source size is the upright size: the decoder
 * applies the EXIF orientation, so the orientation is baked in before any operation, and the output is always
 * written with a normal orientation.
 */
object EditGeometry {
    private class Step(val operation: EditOperation, val sizeBefore: ImageSize, val matrix: Matrix3)

    private class CropPixels(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val size get() = ImageSize(right - left, bottom - top)
    }

    fun outputSize(operations: List<EditOperation>, source: ImageSize): ImageSize =
        operations.fold(source) { size, operation -> sizeAfter(operation, size) }

    fun steps(operations: List<EditOperation>, source: ImageSize): RenderPlan {
        val output = outputSize(operations, source)
        val toOutput = Matrix3.scale(output.width.toDouble(), output.height.toDouble())
        var remaining = Matrix3.IDENTITY
        val strokes = ArrayDeque<RenderStroke>()
        walk(operations, source).asReversed().forEach { step ->
            remaining = step.matrix.then(remaining)
            val operation = step.operation
            if (operation is EditOperation.PenStroke) {
                strokes.addFirst(renderStroke(operation, step.sizeBefore, remaining.then(toOutput)))
            }
        }
        val sourceMatrix = fromPixels(source).then(remaining).then(toOutput)
        return RenderPlan(source, output, sourceMatrix, strokes.toList())
    }

    /** The largest power of two that keeps the decoded image at least as sharp as the output needs. */
    fun decodeSampleSize(plan: RenderPlan): Int {
        val scale = plan.sourceMatrix.averageScale()
        var sample = 1
        while (scale * sample * 2 <= 1.0 && sample * 2 <= plan.sourceSize.longSide) sample *= 2
        return sample
    }

    /** The smallest power of two that brings the longer side down to [maxSide]. */
    fun sampleSizeFor(source: ImageSize, maxSide: Int): Int {
        var sample = 1
        while (source.longSide / sample > maxSide) sample *= 2
        return sample
    }

    private fun walk(operations: List<EditOperation>, source: ImageSize): List<Step> {
        var size = source
        return operations.map { operation ->
            val step = Step(operation, size, matrixFor(operation, size))
            size = sizeAfter(operation, size)
            step
        }
    }

    private fun sizeAfter(operation: EditOperation, size: ImageSize): ImageSize = when (operation) {
        is EditOperation.Rotate -> if (operation.quarterTurns % 2 == 0) size else size.swapped()
        is EditOperation.Crop -> snap(operation.rect, size).size
        is EditOperation.Scale -> ImageSize(operation.targetWidth, operation.targetHeight)
        is EditOperation.PenStroke -> size
    }

    private fun matrixFor(operation: EditOperation, size: ImageSize): Matrix3 = when (operation) {
        is EditOperation.Crop -> snap(operation.rect, size).let { crop ->
            GeometryMapper.crop(
                crop.left.toDouble() / size.width,
                crop.top.toDouble() / size.height,
                crop.size.width.toDouble() / size.width,
                crop.size.height.toDouble() / size.height,
            )
        }

        else -> GeometryMapper.matrixFor(operation)
    }

    /** Crops end on whole pixels, so that cropping does not resample and blur the image. */
    private fun snap(rect: NormalizedRect, size: ImageSize): CropPixels {
        val left = (rect.left * size.width).roundToInt().coerceIn(0, size.width - 1)
        val top = (rect.top * size.height).roundToInt().coerceIn(0, size.height - 1)
        val right = (rect.right * size.width).roundToInt().coerceIn(left + 1, size.width)
        val bottom = (rect.bottom * size.height).roundToInt().coerceIn(top + 1, size.height)
        return CropPixels(left, top, right, bottom)
    }

    private fun fromPixels(size: ImageSize) = Matrix3.scale(1.0 / size.width, 1.0 / size.height)

    private fun renderStroke(stroke: EditOperation.PenStroke, sizeThen: ImageSize, toOutput: Matrix3): RenderStroke {
        val pixelScale = fromPixels(sizeThen).then(toOutput).averageScale()
        return RenderStroke(
            points = stroke.points.map { toOutput.map(it.x.toDouble(), it.y.toDouble()) },
            colorArgb = stroke.colorArgb,
            widthPixels = stroke.widthFraction * sizeThen.longSide * pixelScale,
        )
    }
}
