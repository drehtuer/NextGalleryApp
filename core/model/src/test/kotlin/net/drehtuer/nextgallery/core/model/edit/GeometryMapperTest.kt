package net.drehtuer.nextgallery.core.model.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class GeometryMapperTest {
    private val topLeftArea = NormalizedPoint(0.25f, 0.1f)

    private fun assertMapped(x: Double, y: Double, operations: List<EditOperation>) {
        val point = GeometryMapper.mapPoint(topLeftArea, operations)
        assertEquals(x, point.x, 1e-6)
        assertEquals(y, point.y, 1e-6)
    }

    @Test
    fun rotate_quarterTurn_movesTopLeftToTopRight() {
        assertMapped(0.9, 0.25, listOf(EditOperation.Rotate(1)))
    }

    @Test
    fun rotate_halfTurn_movesTopLeftToBottomRight() {
        assertMapped(0.75, 0.9, listOf(EditOperation.Rotate(2)))
    }

    @Test
    fun rotate_threeQuarterTurns_movesTopLeftToBottomLeft() {
        assertMapped(0.1, 0.75, listOf(EditOperation.Rotate(3)))
    }

    @Test
    fun rotate_fullTurn_changesNothing() {
        assertMapped(0.25, 0.1, listOf(EditOperation.Rotate(4)))
    }

    @Test
    fun rotate_negativeTurn_turnsCounterClockwise() {
        assertMapped(0.1, 0.75, listOf(EditOperation.Rotate(-1)))
    }

    @Test
    fun crop_mapsIntoTheFrame() {
        assertMapped(0.5, 0.2, listOf(EditOperation.Crop(NormalizedRect(0f, 0f, 0.5f, 0.5f))))
    }

    @Test
    fun crop_pointOutsideFrame_endsOutsideTheImage() {
        assertMapped(-0.5, 0.1, listOf(EditOperation.Crop(NormalizedRect(0.5f, 0f, 1f, 1f))))
    }

    @Test
    fun scale_doesNotMoveNormalizedPoints() {
        assertMapped(0.25, 0.1, listOf(EditOperation.Scale(10, 20)))
    }

    @Test
    fun penStroke_doesNotMovePoints() {
        assertMapped(0.25, 0.1, listOf(EditOperation.PenStroke(emptyList(), 0, 0.1f)))
    }

    @Test
    fun rotateThenCrop_cropsTheRotatedImage() {
        // After the turn the point is at (0.9, 0.25); the frame is the right half of the top half.
        val operations = listOf(EditOperation.Rotate(1), EditOperation.Crop(NormalizedRect(0.5f, 0f, 1f, 0.5f)))

        assertMapped(0.8, 0.5, operations)
    }

    @Test
    fun cropThenRotate_rotatesTheCroppedImage() {
        // After the crop the point is at (0.5, 0.2); a quarter turn moves it to (1 - 0.2, 0.5).
        val operations = listOf(EditOperation.Crop(NormalizedRect(0f, 0f, 0.5f, 0.5f)), EditOperation.Rotate(1))

        assertMapped(0.8, 0.5, operations)
    }

    @Test
    fun matrixThrough_noOperations_isIdentity() {
        assertEquals(Matrix3.IDENTITY, GeometryMapper.matrixThrough(emptyList()))
    }
}
