package net.drehtuer.nextgallery.core.model

data class Folder(
    val bucketId: Long,
    val name: String,
    val relativePath: String,
    val itemCount: Int,
    val coverItemId: MediaId,
)
