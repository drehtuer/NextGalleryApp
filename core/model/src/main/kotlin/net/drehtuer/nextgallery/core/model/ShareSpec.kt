package net.drehtuer.nextgallery.core.model

/**
 * What to send through the Android share sheet. `:core:ui` turns it into an intent. With [targetPackage] it goes
 * directly to that app, without the share sheet.
 */
data class ShareSpec(val contentUris: List<String>, val mimeType: String, val targetPackage: String? = null) {
    companion object {
        private const val ANY_IMAGE = "image/*"

        /** Returns null if there is nothing to share. Different image types are shared with a generic image type. */
        fun from(items: List<MediaItem>, targetPackage: String? = null): ShareSpec? {
            if (items.isEmpty()) return null
            val mimeType = items.map { it.mimeType }.distinct().singleOrNull() ?: ANY_IMAGE
            return ShareSpec(items.map { it.contentUri }, mimeType, targetPackage)
        }
    }
}
