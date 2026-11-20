package net.drehtuer.nextgallery.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class ScrollbarDragTest {
    private val scrolled = mutableListOf<Int>()

    /** 1 000 items: a position is the item at that share of the list. */
    private fun target(fraction: Float) = (fraction * 1_000).roundToInt()

    @Test
    fun followsAtOnce_quarterScreen_isTrue() {
        assertTrue(ScrollbarDrag.followsAtOnce(step = 6, visibleCount = 24))
    }

    @Test
    fun followsAtOnce_moreThanAQuarterScreen_isFalse() {
        assertFalse(ScrollbarDrag.followsAtOnce(step = 7, visibleCount = 24))
    }

    @Test
    fun followsAtOnce_upwards_isTheSameAsDownwards() {
        assertFalse(ScrollbarDrag.followsAtOnce(step = -7, visibleCount = 24))
    }

    @Test
    fun followsAtOnce_tinyGrid_stillFollowsOneRow() {
        assertTrue(ScrollbarDrag.followsAtOnce(step = 1, visibleCount = 2))
    }

    @Test
    fun follow_firstTarget_scrollsAtOnce() = runTest {
        launch { ScrollbarDrag.follow(flow { emit(0.5f) }, ::target, { 24 }) { scrolled += it } }
        runCurrent()

        assertEquals(listOf(500), scrolled)
    }

    @Test
    fun follow_smallStep_scrollsAtOnce() = runTest {
        launch {
            ScrollbarDrag.follow(
                flow {
                    emit(0.0f)
                    emit(0.006f)
                },
                ::target,
                { 24 },
            ) { scrolled += it }
        }
        runCurrent()

        assertEquals(listOf(0, 6), scrolled)
    }

    @Test
    fun follow_bigStep_waitsWhileTheThumbMoves() = runTest {
        launch {
            ScrollbarDrag.follow(
                flow {
                    emit(0.0f)
                    emit(0.1f)
                },
                ::target,
                { 24 },
            ) { scrolled += it }
        }
        advanceTimeBy(ScrollbarDrag.SETTLE_MILLIS - 1)

        assertEquals(listOf(0), scrolled)
    }

    @Test
    fun follow_bigStep_scrollsOnceTheThumbRests() = runTest {
        launch {
            ScrollbarDrag.follow(
                flow {
                    emit(0.0f)
                    emit(0.1f)
                },
                ::target,
                { 24 },
            ) { scrolled += it }
        }
        advanceTimeBy(ScrollbarDrag.SETTLE_MILLIS + 1)

        assertEquals(listOf(0, 100), scrolled)
    }

    @Test
    fun follow_fastDrag_scrollsOnlyToWhereItSlowsDown() = runTest {
        val drag = flow {
            emit(0.0f)
            listOf(0.1f, 0.2f, 0.3f).forEach {
                delay(FRAME_MILLIS)
                emit(it)
            }
            delay(FRAME_MILLIS)
            emit(0.302f)
        }
        launch { ScrollbarDrag.follow(drag, ::target, { 24 }) { scrolled += it } }
        advanceTimeBy(4 * FRAME_MILLIS + 1)

        assertEquals(listOf(0, 302), scrolled)
    }

    @Test
    fun follow_dragEnds_dropsTheWaitingScroll() = runTest {
        launch {
            ScrollbarDrag.follow(
                flow {
                    emit(0.0f)
                    emit(0.1f)
                    emit(null)
                },
                ::target,
                { 24 },
            ) { scrolled += it }
        }
        advanceTimeBy(ScrollbarDrag.SETTLE_MILLIS + 1)

        assertEquals(listOf(0), scrolled)
    }

    @Test
    fun follow_targetChangesWithoutThumbMove_doesNotScrollAgain() = runTest {
        var visibleCount = 20
        val shiftingTarget = { _: Float -> 500 + visibleCount++ % 2 }
        launch { ScrollbarDrag.follow(flow { emit(0.5f) }, shiftingTarget, { 24 }) { scrolled += it } }
        advanceTimeBy(ScrollbarDrag.SETTLE_MILLIS + 1)

        assertEquals(listOf(500), scrolled)
    }

    private companion object {
        const val FRAME_MILLIS = 8L
    }
}
