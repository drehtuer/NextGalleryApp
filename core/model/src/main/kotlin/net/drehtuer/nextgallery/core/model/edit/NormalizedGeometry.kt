package net.drehtuer.nextgallery.core.model.edit

private const val MAX_COORDINATE = 1f

/** A position in an image, relative to its size (0..1), so that it does not depend on the resolution. */
data class NormalizedPoint(val x: Float, val y: Float) {
    init {
        require(x in 0f..MAX_COORDINATE && y in 0f..MAX_COORDINATE) { "Point outside the image: $x, $y" }
    }

    companion object {
        /** A pointer can leave the image while drawing; the point stays on the edge. */
        fun clamped(x: Float, y: Float) =
            NormalizedPoint(x.coerceIn(0f, MAX_COORDINATE), y.coerceIn(0f, MAX_COORDINATE))
    }
}

/** A part of an image, relative to its size (0..1). */
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left >= 0f && top >= 0f && right <= MAX_COORDINATE && bottom <= MAX_COORDINATE) {
            "Rect outside the image: $left, $top, $right, $bottom"
        }
        require(left < right && top < bottom) { "Rect is empty: $left, $top, $right, $bottom" }
    }

    val width: Float get() = right - left

    val height: Float get() = bottom - top

    companion object {
        val FULL = NormalizedRect(0f, 0f, MAX_COORDINATE, MAX_COORDINATE)
    }
}
