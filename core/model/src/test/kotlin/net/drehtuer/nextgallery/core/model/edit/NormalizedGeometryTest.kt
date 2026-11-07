package net.drehtuer.nextgallery.core.model.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizedGeometryTest {
    @Test(expected = IllegalArgumentException::class)
    fun point_outsideImage_isRejected() {
        NormalizedPoint(1.5f, 0f)
    }

    @Test
    fun clamped_outsideImage_staysOnEdge() {
        assertEquals(NormalizedPoint(1f, 0f), NormalizedPoint.clamped(1.5f, -0.5f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rect_outsideImage_isRejected() {
        NormalizedRect(-0.1f, 0f, 1f, 1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rect_empty_isRejected() {
        NormalizedRect(0.5f, 0f, 0.5f, 1f)
    }

    @Test
    fun rect_widthAndHeight_areDifferencesOfSides() {
        val rect = NormalizedRect(0.25f, 0.5f, 0.75f, 0.75f)

        assertEquals(listOf(0.5f, 0.25f), listOf(rect.width, rect.height))
    }
}
