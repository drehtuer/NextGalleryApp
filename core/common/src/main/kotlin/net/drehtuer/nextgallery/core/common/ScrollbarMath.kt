package net.drehtuer.nextgallery.core.common

import kotlin.math.roundToInt

/**
 * Maps a fast scrollbar's position (0..1) to the first visible item and back. The last position is
 * `itemCount - visibleCount`, because a grid cannot scroll its last item to the top.
 */
object ScrollbarMath {
    fun indexForFraction(fraction: Float, itemCount: Int, visibleCount: Int): Int =
        (fraction.coerceIn(0f, 1f) * lastFirstIndex(itemCount, visibleCount)).roundToInt()

    fun fractionForIndex(index: Int, itemCount: Int, visibleCount: Int): Float {
        val last = lastFirstIndex(itemCount, visibleCount)
        return if (last == 0) 0f else (index.toFloat() / last).coerceIn(0f, 1f)
    }

    private fun lastFirstIndex(itemCount: Int, visibleCount: Int) = (itemCount - visibleCount).coerceAtLeast(0)
}
