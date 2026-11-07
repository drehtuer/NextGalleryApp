package net.drehtuer.nextgallery.feature.editor

import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.ScaleMath

/** The three linked fields of the scale dialog: changing one updates the others (D50). */
data class ScaleInput(val original: ImageSize, val percent: String, val width: String, val height: String) {
    /** The new size, or null while the fields do not describe a size that can be applied. */
    val target: ImageSize?
        get() {
            val newWidth = width.toPositiveInt() ?: return null
            val newHeight = height.toPositiveInt() ?: return null
            return ImageSize(newWidth, newHeight).takeIf(ScaleMath::isAllowed)
        }

    /** Scaling to the same size would only add a step to the history. */
    val canApply: Boolean get() = target != null && target != original

    fun withPercent(text: String): ScaleInput {
        val digits = text.digits()
        val size = digits.toPositiveInt()?.let { ScaleMath.fromPercent(original, it) }
        return copy(percent = digits, width = size?.width.text(), height = size?.height.text())
    }

    fun withWidth(text: String): ScaleInput {
        val digits = text.digits()
        val size = digits.toPositiveInt()?.let { ScaleMath.fromTargetWidth(original, it) }
        return copy(percent = size.percentText(), width = digits, height = size?.height.text())
    }

    fun withHeight(text: String): ScaleInput {
        val digits = text.digits()
        val size = digits.toPositiveInt()?.let { ScaleMath.fromTargetHeight(original, it) }
        return copy(percent = size.percentText(), width = size?.width.text(), height = digits)
    }

    private fun ImageSize?.percentText() = this?.let { ScaleMath.percentOf(original, it).toString() }.orEmpty()

    private fun Int?.text() = this?.toString().orEmpty()

    private fun String.digits() = filter(Char::isDigit).take(MAX_DIGITS)

    private fun String.toPositiveInt() = toIntOrNull()?.takeIf { it > 0 }

    companion object {
        /** Enough for any allowed size, and small enough not to overflow in the calculations. */
        private const val MAX_DIGITS = 6
        private const val FULL_SIZE_PERCENT = "100"

        fun initial(original: ImageSize) =
            ScaleInput(original, FULL_SIZE_PERCENT, original.width.toString(), original.height.toString())
    }
}
