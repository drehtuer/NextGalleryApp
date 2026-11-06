package net.drehtuer.nextgallery.core.data.index

import net.drehtuer.nextgallery.core.model.RawExif

/** Port: reads raw EXIF tags of an image. Implemented in `:core:media`. */
fun interface ExifSource {
    /** Returns null if the file cannot be read; missing tags are null in the result. */
    suspend fun read(contentUri: String): RawExif?
}
