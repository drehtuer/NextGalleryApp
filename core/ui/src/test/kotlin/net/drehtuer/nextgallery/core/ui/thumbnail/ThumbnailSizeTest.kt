package net.drehtuer.nextgallery.core.ui.thumbnail

import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbnailSizeTest {
    @Test
    fun thumbnailSizePx_smallCells_usesSmallSize() {
        assertEquals(256, thumbnailSizePx(gridWidthPx = 1000, minCellPx = 200))
    }

    @Test
    fun thumbnailSizePx_largeCells_usesLargeSize() {
        assertEquals(512, thumbnailSizePx(gridWidthPx = 1000, minCellPx = 400))
    }

    @Test
    fun thumbnailSizePx_gridNarrowerThanCell_usesOneColumn() {
        assertEquals(256, thumbnailSizePx(gridWidthPx = 100, minCellPx = 400))
    }
}
