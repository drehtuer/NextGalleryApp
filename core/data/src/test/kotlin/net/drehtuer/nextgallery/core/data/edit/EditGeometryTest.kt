package net.drehtuer.nextgallery.core.data.edit

import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.EditOperation.Crop
import net.drehtuer.nextgallery.core.model.edit.EditOperation.PenStroke
import net.drehtuer.nextgallery.core.model.edit.EditOperation.Rotate
import net.drehtuer.nextgallery.core.model.edit.EditOperation.Scale
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.NormalizedPoint
import net.drehtuer.nextgallery.core.model.edit.NormalizedRect
import net.drehtuer.nextgallery.core.model.edit.PlanePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class EditGeometryTest {
    private val source = ImageSize(400, 200)
    private val rightHalf = NormalizedRect(0.5f, 0f, 1f, 1f)

    private fun plan(vararg operations: EditOperation) = EditGeometry.steps(operations.toList(), source)

    private fun stroke(x: Float, y: Float) = PenStroke(listOf(NormalizedPoint(x, y)), COLOR, WIDTH_FRACTION)

    private fun assertPoint(x: Double, y: Double, point: PlanePoint) {
        assertEquals("x", x, point.x, DELTA)
        assertEquals("y", y, point.y, DELTA)
    }

    private fun assertSourceMaps(plan: RenderPlan, sourceX: Double, sourceY: Double, x: Double, y: Double) =
        assertPoint(x, y, plan.sourceMatrix.map(sourceX, sourceY))

    @Test
    fun steps_noOperations_keepsSizeAndPixels() {
        val plan = plan()

        assertEquals(source, plan.outputSize)
        assertSourceMaps(plan, 123.0, 45.0, 123.0, 45.0)
    }

    @Test
    fun steps_rotate_swapsSize() {
        assertEquals(ImageSize(200, 400), plan(Rotate(1)).outputSize)
    }

    @Test
    fun steps_rotate_turnsClockwise() {
        // The top-left corner of the source becomes the top-right corner of the output.
        assertSourceMaps(plan(Rotate(1)), 0.0, 0.0, 200.0, 0.0)
    }

    @Test
    fun steps_rotateHalfTurn_keepsSize() {
        assertEquals(source, plan(Rotate(2)).outputSize)
    }

    @Test
    fun steps_rotateThreeQuarters_movesTopLeftToBottomLeft() {
        assertSourceMaps(plan(Rotate(3)), 0.0, 0.0, 0.0, 400.0)
    }

    @Test
    fun steps_fourQuarterTurns_returnToSource() {
        val plan = plan(Rotate(1), Rotate(1), Rotate(1), Rotate(1))

        assertEquals(source, plan.outputSize)
        assertSourceMaps(plan, 10.0, 20.0, 10.0, 20.0)
    }

    @Test
    fun steps_crop_hasSizeOfFrame() {
        assertEquals(ImageSize(200, 100), plan(Crop(NormalizedRect(0.25f, 0.5f, 0.75f, 1f))).outputSize)
    }

    @Test
    fun steps_crop_movesFrameToOrigin() {
        assertSourceMaps(plan(Crop(NormalizedRect(0.25f, 0.5f, 0.75f, 1f))), 100.0, 100.0, 0.0, 0.0)
    }

    @Test
    fun steps_cropOnFractionsOfAPixel_endsOnWholePixelsWithoutResampling() {
        val plan = EditGeometry.steps(listOf(Crop(NormalizedRect(0.1234f, 0f, 0.4567f, 1f))), ImageSize(100, 100))

        assertEquals(ImageSize(34, 100), plan.outputSize)
        assertEquals(1.0, plan.sourceMatrix.scaleX, DELTA)
    }

    @Test
    fun steps_cropSmallerThanAPixel_keepsOnePixel() {
        val plan = EditGeometry.steps(listOf(Crop(NormalizedRect(0.5f, 0.5f, 0.501f, 0.501f))), ImageSize(10, 10))

        assertEquals(ImageSize(1, 1), plan.outputSize)
    }

    @Test
    fun steps_scale_hasTargetSize() {
        assertEquals(ImageSize(100, 50), plan(Scale(100, 50)).outputSize)
    }

    @Test
    fun steps_scale_shrinksPixels() {
        assertSourceMaps(plan(Scale(100, 50)), 400.0, 200.0, 100.0, 50.0)
    }

    @Test
    fun steps_stroke_keepsSize() {
        assertEquals(source, plan(stroke(0.5f, 0.5f)).outputSize)
    }

    @Test
    fun steps_stroke_hasPointInPixels() {
        assertPoint(200.0, 100.0, plan(stroke(0.5f, 0.5f)).strokes.single().points.single())
    }

    @Test
    fun steps_stroke_widthIsFractionOfLongerSide() {
        assertEquals(4.0, plan(stroke(0.5f, 0.5f)).strokes.single().widthPixels, DELTA)
    }

    @Test
    fun steps_stroke_keepsColor() {
        assertEquals(COLOR, plan(stroke(0.5f, 0.5f)).strokes.single().colorArgb)
    }

    @Test
    fun steps_twoStrokes_keepOrder() {
        val strokes = plan(stroke(0f, 0f), Rotate(1), stroke(1f, 1f)).strokes

        // The first stroke moved with the rotation to the top right, the second one was drawn afterwards.
        assertPoint(200.0, 0.0, strokes[0].points.single())
        assertPoint(200.0, 400.0, strokes[1].points.single())
    }

    @Test
    fun steps_strokeThenRotate_strokeTurnsWithImage() {
        assertPoint(180.0, 100.0, plan(stroke(0.25f, 0.1f), Rotate(1)).strokes.single().points.single())
    }

    @Test
    fun steps_rotateThenStroke_strokeStaysWhereDrawn() {
        assertPoint(50.0, 40.0, plan(Rotate(1), stroke(0.25f, 0.1f)).strokes.single().points.single())
    }

    @Test
    fun steps_strokeThenCrop_strokeMovesWithFrame() {
        assertPoint(100.0, 100.0, plan(stroke(0.75f, 0.5f), Crop(rightHalf)).strokes.single().points.single())
    }

    @Test
    fun steps_strokeThenCrop_strokeOutsideFrameLiesOutsideOutput() {
        val point = plan(stroke(0.25f, 0.5f), Crop(rightHalf)).strokes.single().points.single()

        assertPoint(-100.0, 100.0, point)
    }

    @Test
    fun steps_strokeThenCrop_widthStaysInPixels() {
        assertEquals(4.0, plan(stroke(0.75f, 0.5f), Crop(rightHalf)).strokes.single().widthPixels, DELTA)
    }

    @Test
    fun steps_cropThenStroke_widthIsFractionOfCroppedImage() {
        assertEquals(2.0, plan(Crop(rightHalf), stroke(0.5f, 0.5f)).strokes.single().widthPixels, DELTA)
    }

    @Test
    fun steps_cropThenStroke_pointIsInCroppedImage() {
        assertPoint(100.0, 100.0, plan(Crop(rightHalf), stroke(0.5f, 0.5f)).strokes.single().points.single())
    }

    @Test
    fun steps_strokeThenScale_strokeShrinksWithImage() {
        val stroke = plan(stroke(0.5f, 0.5f), Scale(200, 100)).strokes.single()

        assertPoint(100.0, 50.0, stroke.points.single())
        assertEquals(2.0, stroke.widthPixels, DELTA)
    }

    @Test
    fun steps_scaleThenStroke_widthIsFractionOfScaledImage() {
        assertEquals(2.0, plan(Scale(200, 100), stroke(0.5f, 0.5f)).strokes.single().widthPixels, DELTA)
    }

    @Test
    fun steps_cropThenScale_scalesTheFrame() {
        val plan = plan(Crop(rightHalf), Scale(100, 100))

        assertEquals(ImageSize(100, 100), plan.outputSize)
        assertSourceMaps(plan, 200.0, 0.0, 0.0, 0.0)
        assertSourceMaps(plan, 400.0, 200.0, 100.0, 100.0)
    }

    @Test
    fun steps_scaleThenCrop_cropsTheScaledImage() {
        val plan = plan(Scale(800, 400), Crop(rightHalf))

        assertEquals(ImageSize(400, 400), plan.outputSize)
        assertSourceMaps(plan, 200.0, 0.0, 0.0, 0.0)
        assertSourceMaps(plan, 400.0, 200.0, 400.0, 400.0)
    }

    @Test
    fun steps_rotateThenCrop_cropsTheRotatedImage() {
        // Turned clockwise, the left side of the source is on top, so the top half is the left half of the source.
        val plan = plan(Rotate(1), Crop(NormalizedRect(0f, 0f, 1f, 0.5f)))

        assertEquals(ImageSize(200, 200), plan.outputSize)
        assertSourceMaps(plan, 0.0, 0.0, 200.0, 0.0)
        assertSourceMaps(plan, 200.0, 200.0, 0.0, 200.0)
    }

    @Test
    fun steps_cropThenRotate_turnsTheFrame() {
        val plan = plan(Crop(rightHalf), Rotate(1))

        assertEquals(ImageSize(200, 200), plan.outputSize)
        assertSourceMaps(plan, 200.0, 0.0, 200.0, 0.0)
        assertSourceMaps(plan, 400.0, 0.0, 200.0, 200.0)
    }

    @Test
    fun steps_rotateThenScale_scalesTheTurnedImage() {
        val plan = plan(Rotate(1), Scale(100, 200))

        assertEquals(ImageSize(100, 200), plan.outputSize)
        assertSourceMaps(plan, 0.0, 0.0, 100.0, 0.0)
    }

    @Test
    fun steps_scaleThenRotate_swapsTheScaledSize() {
        assertEquals(ImageSize(50, 100), plan(Scale(100, 50), Rotate(1)).outputSize)
    }

    @Test
    fun steps_allOperations_compose() {
        // 400x200 -> stroke -> turned 200x400 -> top half 200x200 -> scaled 100x100 -> second stroke.
        val plan = plan(
            stroke(0.75f, 0.5f),
            Rotate(1),
            Crop(NormalizedRect(0f, 0f, 1f, 0.5f)),
            Scale(100, 100),
            stroke(0.5f, 0.5f),
        )

        assertEquals(ImageSize(100, 100), plan.outputSize)
        assertSourceMaps(plan, 0.0, 0.0, 100.0, 0.0)
        // (0.75, 0.5) turns to (0.5, 0.75): outside the top half.
        assertPoint(50.0, 150.0, plan.strokes[0].points.single())
        assertEquals(2.0, plan.strokes[0].widthPixels, DELTA)
        assertPoint(50.0, 50.0, plan.strokes[1].points.single())
        assertEquals(1.0, plan.strokes[1].widthPixels, DELTA)
    }

    @Test
    fun outputSize_matchesSteps() {
        val operations = listOf(Rotate(1), Crop(rightHalf), Scale(30, 30))

        assertEquals(EditGeometry.steps(operations, source).outputSize, EditGeometry.outputSize(operations, source))
    }

    @Test
    fun fittedTo_smallEnough_staysTheSame() {
        val plan = plan(Rotate(1))

        assertEquals(plan, plan.fittedTo(400))
    }

    @Test
    fun fittedTo_tooLarge_shrinksLongerSide() {
        assertEquals(ImageSize(100, 50), plan().fittedTo(100).outputSize)
    }

    @Test
    fun fittedTo_tooLarge_keepsSourceSize() {
        assertEquals(source, plan().fittedTo(100).sourceSize)
    }

    @Test
    fun fittedTo_tooLarge_shrinksImageMapping() {
        assertSourceMaps(plan().fittedTo(100), 400.0, 200.0, 100.0, 50.0)
    }

    @Test
    fun fittedTo_tooLarge_shrinksStrokes() {
        val stroke = plan(stroke(0.5f, 0.5f)).fittedTo(100).strokes.single()

        assertPoint(50.0, 25.0, stroke.points.single())
        assertEquals(1.0, stroke.widthPixels, DELTA)
    }

    @Test
    fun fittedTo_extremeShape_keepsOnePixel() {
        val plan = EditGeometry.steps(emptyList(), ImageSize(1000, 1))

        assertEquals(ImageSize(10, 1), plan.fittedTo(10).outputSize)
    }

    @Test
    fun decodeSampleSize_fullSize_isOne() {
        assertEquals(1, EditGeometry.decodeSampleSize(plan(Rotate(1))))
    }

    @Test
    fun decodeSampleSize_quarterSize_isFour() {
        assertEquals(4, EditGeometry.decodeSampleSize(plan(Scale(100, 50))))
    }

    @Test
    fun decodeSampleSize_thirdSize_staysSharperThanOutput() {
        assertEquals(2, EditGeometry.decodeSampleSize(plan(Scale(133, 67))))
    }

    @Test
    fun decodeSampleSize_enlarged_isOne() {
        assertEquals(1, EditGeometry.decodeSampleSize(plan(Scale(800, 400))))
    }

    @Test
    fun sampleSizeFor_smallEnough_isOne() {
        assertEquals(1, EditGeometry.sampleSizeFor(ImageSize(2048, 100), 2048))
    }

    @Test
    fun sampleSizeFor_justLarger_isTwo() {
        assertEquals(2, EditGeometry.sampleSizeFor(ImageSize(4000, 3000), 2048))
    }

    @Test
    fun sampleSizeFor_muchLarger_reachesMaxSide() {
        assertEquals(4, EditGeometry.sampleSizeFor(ImageSize(3000, 8160), 2048))
    }

    private companion object {
        const val COLOR = 0x11223344
        const val WIDTH_FRACTION = 0.01f
        const val DELTA = 1e-3
    }
}
