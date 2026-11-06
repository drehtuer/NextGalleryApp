package net.drehtuer.nextgallery.core.model

/** The list of images a viewer pages through. The timeline and the map add their own cases in Phases 5 and 6. */
sealed interface MediaSource {
    data class Folder(val bucketId: Long) : MediaSource
}
