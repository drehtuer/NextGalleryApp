package net.drehtuer.nextgallery.core.model.edit

enum class AspectRatio { FREE, ORIGINAL, SQUARE, R4_3, R16_9 }

/** The part of the crop frame a drag moves. [x] and [y] tell which sides: -1 left/top, 1 right/bottom, 0 none. */
enum class CropHandle(val x: Int, val y: Int) {
    TOP_LEFT(-1, -1),
    TOP(0, -1),
    TOP_RIGHT(1, -1),
    LEFT(-1, 0),
    INSIDE(0, 0),
    RIGHT(1, 0),
    BOTTOM_LEFT(-1, 1),
    BOTTOM(0, 1),
    BOTTOM_RIGHT(1, 1),
}

/** A drag distance, relative to the size of the image. */
data class NormalizedDistance(val x: Float, val y: Float)

object CropMath {
    /** The frame never gets smaller than this part of the image by dragging. */
    const val MIN_SIZE = 0.05f

    private const val FOUR_THREE = 4f / 3f
    private const val SIXTEEN_NINE = 16f / 9f

    /** Width divided by height, or null without a limit. 4:3 and 16:9 follow the image: upright gets 3:4, 9:16. */
    fun widthPerHeight(ratio: AspectRatio, bounds: ImageSize): Float? {
        val landscape = bounds.width >= bounds.height
        return when (ratio) {
            AspectRatio.FREE -> null
            AspectRatio.ORIGINAL -> bounds.width.toFloat() / bounds.height
            AspectRatio.SQUARE -> 1f
            AspectRatio.R4_3 -> if (landscape) FOUR_THREE else 1 / FOUR_THREE
            AspectRatio.R16_9 -> if (landscape) SIXTEEN_NINE else 1 / SIXTEEN_NINE
        }
    }

    /** The largest frame with this ratio inside [rect], around the same centre. [bounds] is the image in pixels. */
    fun constrain(rect: NormalizedRect, ratio: AspectRatio, bounds: ImageSize): NormalizedRect {
        val target = widthPerHeight(ratio, bounds) ?: return rect
        val width = rect.width * bounds.width
        val height = rect.height * bounds.height
        val fittedWidth = minOf(width, height * target) / bounds.width
        val fittedHeight = minOf(height, width / target) / bounds.height
        val centerX = (rect.left + rect.right) / 2
        val centerY = (rect.top + rect.bottom) / 2
        return NormalizedRect(
            left = (centerX - fittedWidth / 2).coerceAtLeast(rect.left),
            top = (centerY - fittedHeight / 2).coerceAtLeast(rect.top),
            right = (centerX + fittedWidth / 2).coerceAtMost(rect.right),
            bottom = (centerY + fittedHeight / 2).coerceAtMost(rect.bottom),
        )
    }

    /** The handle at a position, or null outside the frame. Tolerances are normalized like the position. */
    fun handleAt(rect: NormalizedRect, x: Float, y: Float, toleranceX: Float, toleranceY: Float): CropHandle? {
        val side = side(x, rect.left, rect.right, toleranceX)
        val edge = side(y, rect.top, rect.bottom, toleranceY)
        return if (side == null || edge == null) null else CropHandle.entries.first { it.x == side && it.y == edge }
    }

    /** The frame after dragging [handle] by a normalized distance. It stays in the image and keeps [ratio]. */
    fun drag(
        rect: NormalizedRect,
        handle: CropHandle,
        distance: NormalizedDistance,
        ratio: AspectRatio,
        bounds: ImageSize,
    ): NormalizedRect = CropDrag.drag(rect, handle, distance, ratio, bounds)

    private fun side(position: Float, start: Float, end: Float, tolerance: Float): Int? = when {
        position < start - tolerance || position > end + tolerance -> null
        position <= start + tolerance -> -1
        position >= end - tolerance -> 1
        else -> 0
    }
}
