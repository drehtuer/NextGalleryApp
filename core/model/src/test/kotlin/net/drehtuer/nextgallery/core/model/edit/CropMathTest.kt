package net.drehtuer.nextgallery.core.model.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CropMathTest {
    private val landscape = ImageSize(4000, 3000)
    private val portrait = ImageSize(3000, 4000)
    private val squareImage = ImageSize(1000, 1000)
    private val middle = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f)

    private fun assertRect(expected: NormalizedRect, actual: NormalizedRect) {
        assertEquals(expected.left, actual.left, 1e-3f)
        assertEquals(expected.top, actual.top, 1e-3f)
        assertEquals(expected.right, actual.right, 1e-3f)
        assertEquals(expected.bottom, actual.bottom, 1e-3f)
    }

    private fun pixelRatio(rect: NormalizedRect, bounds: ImageSize) =
        rect.width * bounds.width / (rect.height * bounds.height)

    private fun drag(
        handle: CropHandle,
        dx: Float,
        dy: Float,
        start: NormalizedRect = middle,
        ratio: AspectRatio = AspectRatio.FREE,
        bounds: ImageSize = landscape,
    ) = CropMath.drag(start, handle, NormalizedDistance(dx, dy), ratio, bounds)

    @Test
    fun widthPerHeight_free_hasNoLimit() {
        assertNull(CropMath.widthPerHeight(AspectRatio.FREE, landscape))
    }

    @Test
    fun widthPerHeight_fourThreeOnUprightImage_isThreeFour() {
        assertEquals(0.75f, CropMath.widthPerHeight(AspectRatio.R4_3, portrait)!!, 1e-6f)
    }

    @Test
    fun widthPerHeight_sixteenNineOnUprightImage_isNineSixteen() {
        assertEquals(9f / 16f, CropMath.widthPerHeight(AspectRatio.R16_9, portrait)!!, 1e-6f)
    }

    @Test
    fun constrain_free_keepsRect() {
        assertEquals(middle, CropMath.constrain(middle, AspectRatio.FREE, landscape))
    }

    @Test
    fun constrain_original_fullImageStaysFull() {
        assertRect(NormalizedRect.FULL, CropMath.constrain(NormalizedRect.FULL, AspectRatio.ORIGINAL, landscape))
    }

    @Test
    fun constrain_square_isSquareInPixels() {
        val rect = CropMath.constrain(NormalizedRect.FULL, AspectRatio.SQUARE, landscape)

        assertEquals(1f, pixelRatio(rect, landscape), 1e-3f)
    }

    @Test
    fun constrain_square_staysCentred() {
        val rect = CropMath.constrain(NormalizedRect.FULL, AspectRatio.SQUARE, landscape)

        assertRect(NormalizedRect(0.125f, 0f, 0.875f, 1f), rect)
    }

    @Test
    fun constrain_fourThree_hasTheRatio() {
        val rect = CropMath.constrain(middle, AspectRatio.R4_3, squareImage)

        assertEquals(4f / 3f, pixelRatio(rect, squareImage), 1e-3f)
    }

    @Test
    fun constrain_sixteenNine_narrowsTheHeight() {
        val rect = CropMath.constrain(NormalizedRect.FULL, AspectRatio.R16_9, landscape)

        // 16:9 on 4000 px is 2250 px of the 3000 px height.
        assertRect(NormalizedRect(0f, 0.125f, 1f, 0.875f), rect)
    }

    @Test
    fun handleAt_corner_returnsCorner() {
        assertEquals(CropHandle.TOP_LEFT, CropMath.handleAt(middle, 0.26f, 0.24f, 0.05f, 0.05f))
    }

    @Test
    fun handleAt_edge_returnsEdge() {
        assertEquals(CropHandle.RIGHT, CropMath.handleAt(middle, 0.75f, 0.5f, 0.05f, 0.05f))
    }

    @Test
    fun handleAt_middle_returnsInside() {
        assertEquals(CropHandle.INSIDE, CropMath.handleAt(middle, 0.5f, 0.5f, 0.05f, 0.05f))
    }

    @Test
    fun handleAt_outside_returnsNull() {
        assertNull(CropMath.handleAt(middle, 0.1f, 0.5f, 0.05f, 0.05f))
    }

    @Test
    fun drag_inside_movesFrame() {
        assertRect(NormalizedRect(0.35f, 0.15f, 0.85f, 0.65f), drag(CropHandle.INSIDE, 0.1f, -0.1f))
    }

    @Test
    fun drag_insidePastEdge_stopsAtEdgeWithSameSize() {
        assertRect(NormalizedRect(0.5f, 0.25f, 1f, 0.75f), drag(CropHandle.INSIDE, 0.9f, 0f))
    }

    @Test
    fun drag_cornerFree_movesTwoSides() {
        assertRect(NormalizedRect(0.15f, 0.05f, 0.75f, 0.75f), drag(CropHandle.TOP_LEFT, -0.1f, -0.2f))
    }

    @Test
    fun drag_edgeFree_movesOneSide() {
        assertRect(NormalizedRect(0.25f, 0.25f, 0.75f, 0.85f), drag(CropHandle.BOTTOM, 0.3f, 0.1f))
    }

    @Test
    fun drag_pastImageEdge_stopsAtEdge() {
        assertEquals(1f, drag(CropHandle.RIGHT, 0.9f, 0f).right, 1e-6f)
    }

    @Test
    fun drag_pastOppositeSide_keepsMinimumSize() {
        assertEquals(CropMath.MIN_SIZE, drag(CropHandle.LEFT, 0.9f, 0f).width, 1e-5f)
    }

    @Test
    fun drag_cornerLocked_keepsRatio() {
        val start = CropMath.constrain(middle, AspectRatio.SQUARE, landscape)

        val rect = drag(CropHandle.BOTTOM_RIGHT, -0.1f, 0f, start, AspectRatio.SQUARE)

        assertEquals(1f, pixelRatio(rect, landscape), 1e-3f)
    }

    @Test
    fun drag_cornerLocked_keepsOppositeCorner() {
        val start = CropMath.constrain(middle, AspectRatio.SQUARE, landscape)

        val rect = drag(CropHandle.BOTTOM_RIGHT, -0.1f, 0f, start, AspectRatio.SQUARE)

        assertEquals(listOf(start.left, start.top), listOf(rect.left, rect.top))
    }

    @Test
    fun drag_cornerLockedVerticalDrag_followsTheHeight() {
        val start = CropMath.constrain(middle, AspectRatio.SQUARE, landscape)

        val rect = drag(CropHandle.BOTTOM_RIGHT, 0f, -0.1f, start, AspectRatio.SQUARE)

        assertEquals(start.height - 0.1f, rect.height, 1e-3f)
    }

    @Test
    fun drag_edgeLocked_growsEvenlyAroundCentre() {
        val rect = drag(CropHandle.RIGHT, 0.1f, 0f, ratio = AspectRatio.SQUARE, bounds = squareImage)

        assertRect(NormalizedRect(0.25f, 0.2f, 0.85f, 0.8f), rect)
    }

    @Test
    fun drag_lockedPastImageEdge_staysInsideWithRatio() {
        val rect = drag(CropHandle.BOTTOM_RIGHT, 2f, 2f, ratio = AspectRatio.SQUARE, bounds = squareImage)

        assertRect(NormalizedRect(0.25f, 0.25f, 1f, 1f), rect)
    }

    @Test
    fun drag_topLeftLocked_keepsBottomRight() {
        val rect = drag(CropHandle.TOP_LEFT, -0.1f, -0.1f, ratio = AspectRatio.SQUARE, bounds = squareImage)

        assertRect(NormalizedRect(0.15f, 0.15f, 0.75f, 0.75f), rect)
    }

    @Test
    fun drag_topEdgeLocked_followsTheHeight() {
        val rect = drag(CropHandle.TOP, 0f, -0.1f, ratio = AspectRatio.SQUARE, bounds = squareImage)

        assertRect(NormalizedRect(0.2f, 0.15f, 0.8f, 0.75f), rect)
    }

    @Test
    fun drag_lockedBelowMinimum_keepsFrame() {
        val small = NormalizedRect(0.5f, 0.5f, 0.55f, 0.55f)

        // The free drag would stop at the minimum width, but this image is so tall that the height falls below it.
        val rect = drag(CropHandle.BOTTOM_RIGHT, -0.04f, 0f, small, AspectRatio.SQUARE, ImageSize(1000, 2000))

        assertSame(small, rect)
    }
}
