package net.drehtuer.nextgallery.core.model.edit

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Sizes for the scale tool. Proportions are always kept; no side gets smaller than a pixel. */
object ScaleMath {
    /** Bigger images cannot be encoded reliably, whatever the memory. */
    const val MAX_SIDE = 32_000

    private const val PERCENT = 100.0
    private const val PIXELS_PER_MEGAPIXEL = 1_000_000.0

    fun fromPercent(size: ImageSize, percent: Int) = ImageSize(
        side(size.width * (percent / PERCENT)),
        side(size.height * (percent / PERCENT)),
    )

    fun fromTargetWidth(size: ImageSize, width: Int) =
        ImageSize(width, side(width.toDouble() * size.height / size.width))

    fun fromTargetHeight(size: ImageSize, height: Int) =
        ImageSize(side(height.toDouble() * size.width / size.height), height)

    fun percentOf(size: ImageSize, target: ImageSize): Int =
        (target.width * PERCENT / size.width).roundToLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun isAllowed(target: ImageSize): Boolean = target.longSide <= MAX_SIDE

    /** Rounded, at least 1 for any real image, for texts like "12 MP". */
    fun megapixels(size: ImageSize): Int = (size.pixels / PIXELS_PER_MEGAPIXEL).roundToInt().coerceAtLeast(1)

    private fun side(value: Double): Int = value.roundToLong().coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
}
