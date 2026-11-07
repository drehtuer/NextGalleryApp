package net.drehtuer.nextgallery.core.model

/** The list of images a viewer pages through. */
sealed interface MediaSource {
    data class Folder(val bucketId: Long) : MediaSource

    /** The images of several folders together (D55). */
    data class VirtualFolder(val bucketIds: List<Long>) : MediaSource

    /** All indexed images, by local date. */
    data object Timeline : MediaSource

    /** The images of a map cluster; [bounds] is [Cluster.bounds]. */
    data class Map(val bounds: GeoBounds) : MediaSource
}
