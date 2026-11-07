package net.drehtuer.nextgallery.core.model.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleMathTest {
    private val size = ImageSize(4000, 3000)

    @Test
    fun fromPercent_halvesBothSides() {
        assertEquals(ImageSize(2000, 1500), ScaleMath.fromPercent(size, 50))
    }

    @Test
    fun fromPercent_above100_enlarges() {
        assertEquals(ImageSize(8000, 6000), ScaleMath.fromPercent(size, 200))
    }

    @Test
    fun fromPercent_tiny_keepsAtLeastOnePixel() {
        assertEquals(ImageSize(1, 1), ScaleMath.fromPercent(ImageSize(10, 10), 1))
    }

    @Test
    fun fromTargetWidth_keepsAspect() {
        assertEquals(ImageSize(1000, 750), ScaleMath.fromTargetWidth(size, 1000))
    }

    @Test
    fun fromTargetHeight_keepsAspect() {
        assertEquals(ImageSize(1000, 750), ScaleMath.fromTargetHeight(size, 750))
    }

    @Test
    fun fromTargetWidth_thinImage_keepsAtLeastOnePixel() {
        assertEquals(ImageSize(10, 1), ScaleMath.fromTargetWidth(ImageSize(1000, 1), 10))
    }

    @Test
    fun percentOf_returnsRoundedPercentage() {
        assertEquals(25, ScaleMath.percentOf(size, ImageSize(1000, 750)))
    }

    @Test
    fun isAllowed_atLimit() {
        assertTrue(ScaleMath.isAllowed(ImageSize(ScaleMath.MAX_SIDE, 1)))
    }

    @Test
    fun isAllowed_aboveLimit() {
        assertFalse(ScaleMath.isAllowed(ImageSize(1, ScaleMath.MAX_SIDE + 1)))
    }

    @Test
    fun megapixels_rounds() {
        assertEquals(12, ScaleMath.megapixels(size))
    }

    @Test
    fun megapixels_smallImage_isAtLeastOne() {
        assertEquals(1, ScaleMath.megapixels(ImageSize(10, 10)))
    }
}
