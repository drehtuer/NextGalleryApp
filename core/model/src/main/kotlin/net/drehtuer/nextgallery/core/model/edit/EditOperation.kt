package net.drehtuer.nextgallery.core.model.edit

/**
 * One step of a non-destructive edit (D12). Coordinates are relative to the image as it looked just before the
 * operation, so they do not depend on the resolution.
 */
sealed interface EditOperation {
    /** Clockwise, in steps of 90°. */
    data class Rotate(val quarterTurns: Int) : EditOperation

    data class Crop(val rect: NormalizedRect) : EditOperation

    data class Scale(val targetWidth: Int, val targetHeight: Int) : EditOperation

    /** [widthFraction] is the pen width relative to the longer side of the image. */
    data class PenStroke(val points: List<NormalizedPoint>, val colorArgb: Int, val widthFraction: Float) :
        EditOperation
}
