package net.drehtuer.nextgallery.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

/**
 * Moves a grid behind a fast scrollbar's thumb (D63). A jump of more than a screen composes every cell anew, which
 * took longer than a frame, so while the thumb moves fast the grid waits. It follows again once the thumb slows down
 * near its destination or rests for [SETTLE_MILLIS]. The scrollbar itself scrolls to the final position when the drag
 * ends.
 */
object ScrollbarDrag {
    const val SETTLE_MILLIS = 80L
    private const val SCREEN_PARTS = 4

    /** Up to a quarter of a screen per drag event keeps most cells, so the grid can follow at once. */
    fun followsAtOnce(step: Int, visibleCount: Int): Boolean =
        abs(step) <= (visibleCount / SCREEN_PARTS).coerceAtLeast(1)

    /**
     * [fractions] is the thumb's position, or null when no drag is going on; null drops a scroll that is still
     * waiting. [targetFor] gives the first item for a position. Only a new position scrolls: the target also depends
     * on how many items are visible, which scrolling changes, so following the target itself scrolled back and forth
     * without end within one frame.
     */
    suspend fun follow(
        fractions: Flow<Float?>,
        targetFor: (Float) -> Int,
        visibleCount: () -> Int,
        scrollTo: suspend (Int) -> Unit,
    ) {
        var previous: Int? = null
        fractions.collectLatest { fraction ->
            val target = fraction?.let(targetFor)
            val last = previous
            previous = target
            if (target == null) return@collectLatest
            if (last != null && !followsAtOnce(target - last, visibleCount())) delay(SETTLE_MILLIS)
            scrollTo(target)
        }
    }
}
