package net.drehtuer.nextgallery.core.model.edit

import kotlin.math.abs

/** Dragging the crop frame; see [CropMath.drag]. */
internal object CropDrag {
    fun drag(
        rect: NormalizedRect,
        handle: CropHandle,
        distance: NormalizedDistance,
        ratio: AspectRatio,
        bounds: ImageSize,
    ): NormalizedRect {
        val target = CropMath.widthPerHeight(ratio, bounds)
        return when {
            handle == CropHandle.INSIDE -> move(rect, distance)
            target == null -> dragEdges(rect, handle, distance)
            else -> dragWithRatio(rect, handle, distance, target, bounds)
        }
    }

    private fun dragWithRatio(
        rect: NormalizedRect,
        handle: CropHandle,
        distance: NormalizedDistance,
        target: Float,
        bounds: ImageSize,
    ): NormalizedRect {
        val free = dragEdges(rect, handle, distance)
        val followWidth = handle.y == 0 ||
            (handle.x != 0 && abs(distance.x * bounds.width) >= abs(distance.y * bounds.height))
        val wanted = if (followWidth) free.width * bounds.width else free.height * bounds.height * target
        return fixedRatioFrame(rect, handle, wanted, target, bounds)
    }

    private fun move(rect: NormalizedRect, distance: NormalizedDistance): NormalizedRect {
        val x = distance.x.coerceIn(-rect.left, 1f - rect.right)
        val y = distance.y.coerceIn(-rect.top, 1f - rect.bottom)
        return NormalizedRect(
            left = (rect.left + x).coerceAtLeast(0f),
            top = (rect.top + y).coerceAtLeast(0f),
            right = (rect.right + x).coerceAtMost(1f),
            bottom = (rect.bottom + y).coerceAtMost(1f),
        )
    }

    private fun dragEdges(rect: NormalizedRect, handle: CropHandle, distance: NormalizedDistance) = NormalizedRect(
        left = if (handle.x < 0) lowerEdge(rect.left + distance.x, rect.right) else rect.left,
        top = if (handle.y < 0) lowerEdge(rect.top + distance.y, rect.bottom) else rect.top,
        right = if (handle.x > 0) upperEdge(rect.right + distance.x, rect.left) else rect.right,
        bottom = if (handle.y > 0) upperEdge(rect.bottom + distance.y, rect.top) else rect.bottom,
    )

    private fun lowerEdge(position: Float, opposite: Float) =
        minOf(position.coerceAtLeast(0f), opposite - CropMath.MIN_SIZE).coerceAtLeast(0f)

    private fun upperEdge(position: Float, opposite: Float) =
        maxOf(position.coerceAtMost(1f), opposite + CropMath.MIN_SIZE).coerceAtMost(1f)

    /** The opposite side stays where it is; a side handle grows the frame evenly around the centre of its edge. */
    private fun fixedRatioFrame(
        rect: NormalizedRect,
        handle: CropHandle,
        wantedWidth: Float,
        target: Float,
        bounds: ImageSize,
    ): NormalizedRect {
        val roomX = room(handle.x, rect.left, rect.right) * bounds.width
        val roomY = room(handle.y, rect.top, rect.bottom) * bounds.height
        val widthPixels = minOf(wantedWidth, roomX, roomY * target)
        val width = widthPixels / bounds.width
        val height = widthPixels / target / bounds.height
        if (width < CropMath.MIN_SIZE || height < CropMath.MIN_SIZE) return rect
        val (left, right) = span(handle.x, rect.left, rect.right, width)
        val (top, bottom) = span(handle.y, rect.top, rect.bottom, height)
        return NormalizedRect(
            left.coerceAtLeast(0f),
            top.coerceAtLeast(0f),
            right.coerceAtMost(1f),
            bottom.coerceAtMost(1f),
        )
    }

    private fun room(side: Int, start: Float, end: Float): Float = when {
        side < 0 -> end
        side > 0 -> 1f - start
        else -> minOf(start + end, 2f - start - end)
    }

    private fun span(side: Int, start: Float, end: Float, size: Float): Pair<Float, Float> = when {
        side < 0 -> end - size to end
        side > 0 -> start to start + size
        else -> (start + end - size) / 2 to (start + end + size) / 2
    }
}
