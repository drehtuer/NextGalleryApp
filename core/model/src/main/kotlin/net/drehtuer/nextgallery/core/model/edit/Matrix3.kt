package net.drehtuer.nextgallery.core.model.edit

import kotlin.math.abs
import kotlin.math.sqrt

/** A position that may lie outside the image, e.g. a pen point that a later crop removed. */
data class PlanePoint(val x: Double, val y: Double)

/**
 * A 3×3 affine matrix with the last row (0, 0, 1):
 * `x' = scaleX·x + skewX·y + translateX`, `y' = skewY·x + scaleY·y + translateY`.
 */
data class Matrix3(
    val scaleX: Double,
    val skewX: Double,
    val translateX: Double,
    val skewY: Double,
    val scaleY: Double,
    val translateY: Double,
) {
    fun map(x: Double, y: Double) = PlanePoint(
        scaleX * x + skewX * y + translateX,
        skewY * x + scaleY * y + translateY,
    )

    /** The matrix that first applies this one and then [next]. */
    fun then(next: Matrix3) = Matrix3(
        scaleX = next.scaleX * scaleX + next.skewX * skewY,
        skewX = next.scaleX * skewX + next.skewX * scaleY,
        translateX = next.scaleX * translateX + next.skewX * translateY + next.translateX,
        skewY = next.skewY * scaleX + next.scaleY * skewY,
        scaleY = next.skewY * skewX + next.scaleY * scaleY,
        translateY = next.skewY * translateX + next.scaleY * translateY + next.translateY,
    )

    /** How much lengths grow on average; rotations and translations do not change it. */
    fun averageScale(): Double = sqrt(abs(scaleX * scaleY - skewX * skewY))

    companion object {
        val IDENTITY = Matrix3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0)

        fun scale(x: Double, y: Double) = Matrix3(x, 0.0, 0.0, 0.0, y, 0.0)
    }
}
