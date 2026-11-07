package net.drehtuer.nextgallery.core.model

/** The list of images a viewer pages through. The map adds its own case in Phase 6. */
sealed interface MediaSource {
    data class Folder(val bucketId: Long) : MediaSource

    /** All indexed images, by local date. */
    data object Timeline : MediaSource
}
