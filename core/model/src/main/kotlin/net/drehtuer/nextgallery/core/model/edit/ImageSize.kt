package net.drehtuer.nextgallery.core.model.edit

/** Size of an image in pixels, as it is shown: EXIF rotation is already applied. */
data class ImageSize(val width: Int, val height: Int) {
    init {
        require(width > 0 && height > 0) { "Size must be positive: $width x $height" }
    }

    val pixels: Long get() = width.toLong() * height

    val longSide: Int get() = maxOf(width, height)

    fun swapped() = ImageSize(height, width)
}
