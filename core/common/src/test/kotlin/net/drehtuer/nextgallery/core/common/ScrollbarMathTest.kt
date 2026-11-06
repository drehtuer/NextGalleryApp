package net.drehtuer.nextgallery.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollbarMathTest {
    @Test
    fun indexForFraction_top_returnsFirstItem() {
        assertEquals(0, ScrollbarMath.indexForFraction(0f, itemCount = 100, visibleCount = 10))
    }

    @Test
    fun indexForFraction_bottom_returnsLastReachableItem() {
        assertEquals(90, ScrollbarMath.indexForFraction(1f, itemCount = 100, visibleCount = 10))
    }

    @Test
    fun indexForFraction_middle_returnsMiddleItem() {
        assertEquals(45, ScrollbarMath.indexForFraction(0.5f, itemCount = 100, visibleCount = 10))
    }

    @Test
    fun indexForFraction_outOfRange_isClamped() {
        assertEquals(90, ScrollbarMath.indexForFraction(1.5f, itemCount = 100, visibleCount = 10))
    }

    @Test
    fun indexForFraction_allItemsVisible_returnsFirstItem() {
        assertEquals(0, ScrollbarMath.indexForFraction(1f, itemCount = 5, visibleCount = 10))
    }

    @Test
    fun fractionForIndex_lastReachableItem_returnsOne() {
        assertEquals(1f, ScrollbarMath.fractionForIndex(90, itemCount = 100, visibleCount = 10), 0f)
    }

    @Test
    fun fractionForIndex_middleItem_returnsHalf() {
        assertEquals(0.5f, ScrollbarMath.fractionForIndex(45, itemCount = 100, visibleCount = 10), 0f)
    }

    @Test
    fun fractionForIndex_allItemsVisible_returnsZero() {
        assertEquals(0f, ScrollbarMath.fractionForIndex(0, itemCount = 5, visibleCount = 10), 0f)
    }
}
