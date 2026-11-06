package net.drehtuer.nextgallery.core.data.index

/** Progress of the incremental sync for one MediaStore volume. */
data class SyncState(
    val volumeName: String,
    val lastGeneration: Long,
    val mediaStoreVersion: String,
    val lastFullScanMillis: Long,
)
