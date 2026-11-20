package net.drehtuer.nextgallery.feature.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RailRowTest {
    @Test
    fun railRow_notDragging_isTheMiddleRow() {
        assertEquals(40, railRow(dragTarget = null, firstVisible = 30, middle = 40, rowCount = 1000))
    }

    @Test
    fun railRow_dragging_isAsFarBelowTheTargetAsTheMiddleIsBelowTheTop() {
        assertEquals(510, railRow(dragTarget = 500, firstVisible = 30, middle = 40, rowCount = 1000))
    }

    @Test
    fun railRow_draggingToTheEnd_staysOnTheLastRow() {
        assertEquals(999, railRow(dragTarget = 995, firstVisible = 30, middle = 40, rowCount = 1000))
    }

    @Test
    fun railRow_nothingLaidOut_isNull() {
        assertNull(railRow(dragTarget = 500, firstVisible = 0, middle = null, rowCount = 1000))
    }
}
