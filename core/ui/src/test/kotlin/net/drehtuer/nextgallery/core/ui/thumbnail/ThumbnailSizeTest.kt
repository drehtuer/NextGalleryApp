package net.drehtuer.nextgallery.core.ui.thumbnail

import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbnailSizeTest {
    @Test
    fun thumbnailSizePx_smallCells_usesSmallSize() {
        assertEquals(256, thumbnailSizePx(gridWidthPx = 1000, columns = 5))
    }

    @Test
    fun thumbnailSizePx_largeCells_usesLargeSize() {
        assertEquals(512, thumbnailSizePx(gridWidthPx = 1080, columns = 3))
    }

    @Test
    fun thumbnailSizePx_noColumns_countsAsOneColumn() {
        assertEquals(512, thumbnailSizePx(gridWidthPx = 1000, columns = 0))
    }
}
