package net.drehtuer.nextgallery.core.model

/** Image types the app shows (D8) and the subset it can edit (D29). */
object SupportedMimeTypes {
    val all: Set<String> = setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/gif")

    private val editable: Set<String> = setOf("image/jpeg", "image/png")

    fun isSupported(mimeType: String): Boolean = mimeType.lowercase() in all

    fun isEditable(mimeType: String): Boolean = mimeType.lowercase() in editable
}
