package net.drehtuer.nextgallery.core.model

/** What to send through the Android share sheet. `:core:ui` turns it into an intent. */
data class ShareSpec(val contentUris: List<String>, val mimeType: String) {
    companion object {
        private const val ANY_IMAGE = "image/*"

        /** Returns null if there is nothing to share. Different image types are shared with a generic image type. */
        fun from(items: List<MediaItem>): ShareSpec? {
            if (items.isEmpty()) return null
            val mimeType = items.map { it.mimeType }.distinct().singleOrNull() ?: ANY_IMAGE
            return ShareSpec(items.map { it.contentUri }, mimeType)
        }
    }
}
