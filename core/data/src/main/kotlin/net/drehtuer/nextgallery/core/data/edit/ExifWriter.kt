package net.drehtuer.nextgallery.core.data.edit

/** Port: carries EXIF tags from a source image to an edited one. Implemented in `:core:media`. */
interface ExifWriter {
    /** The values of [tags] that are set in the image at [contentUri]. */
    suspend fun readTags(contentUri: String, tags: Set<String>): Map<String, String>

    suspend fun writeTags(image: EncodedImage, tags: Map<String, String>)
}
