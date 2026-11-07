package net.drehtuer.nextgallery.core.model

/** The list of images a viewer pages through. The timeline adds its own case in Phase 5. */
sealed interface MediaSource {
    data class Folder(val bucketId: Long) : MediaSource

    /** The images of a map cluster; [bounds] is [Cluster.bounds]. */
    data class Map(val bounds: GeoBounds) : MediaSource
}
