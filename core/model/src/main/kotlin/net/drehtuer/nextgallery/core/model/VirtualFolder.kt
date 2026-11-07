package net.drehtuer.nextgallery.core.model

/**
 * Shows the images of several real folders together (D55). A member that is not indexed at the moment is ignored, and
 * counts again once it is back.
 */
data class VirtualFolder(val id: String, val name: String, val bucketIds: List<Long>)

/** A virtual folder with what its members hold right now. [coverItem] is the newest image, null if there is none. */
data class VirtualFolderSummary(val folder: VirtualFolder, val itemCount: Int, val coverItem: MediaItem?)
