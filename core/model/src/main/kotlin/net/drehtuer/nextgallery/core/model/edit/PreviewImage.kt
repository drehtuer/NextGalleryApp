package net.drehtuer.nextgallery.core.model.edit

/**
 * A rendered image that crosses the ports as an opaque handle. The Android adapter puts a `Bitmap` in it and
 * `:core:ui` draws it; everything in between only passes it on (D25).
 */
class PreviewImage(val handle: Any, val width: Int, val height: Int)
