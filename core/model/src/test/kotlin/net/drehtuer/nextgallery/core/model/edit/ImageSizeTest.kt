package net.drehtuer.nextgallery.core.model.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizeTest {
    @Test(expected = IllegalArgumentException::class)
    fun init_zeroSide_isRejected() {
        ImageSize(0, 10)
    }

    @Test
    fun pixels_hugeImage_doesNotOverflow() {
        assertEquals(10_000_000_000L, ImageSize(100_000, 100_000).pixels)
    }

    @Test
    fun longSide_isTheLargerSide() {
        assertEquals(30, ImageSize(10, 30).longSide)
    }

    @Test
    fun swapped_exchangesSides() {
        assertEquals(ImageSize(30, 10), ImageSize(10, 30).swapped())
    }
}
