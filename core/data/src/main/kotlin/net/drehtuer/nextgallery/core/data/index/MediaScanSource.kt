package net.drehtuer.nextgallery.core.data.index

import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.model.ScannedImage

/** Port: reads images from MediaStore. Implemented in `:core:media`. */
interface MediaScanSource {
    suspend fun volumes(): Set<String>

    /** Opaque per-app value; only compared for equality (D40). */
    suspend fun mediaStoreVersion(volume: String): String

    suspend fun currentGeneration(volume: String): Long

    /**
     * [folders] are relative paths and must not be empty: only images in them and below them are read (D55), for
     * this and for the two functions after it.
     */
    fun fullScan(volume: String, folders: List<String>): Flow<List<ScannedImage>>

    fun changesSince(volume: String, generation: Long, folders: List<String>): Flow<List<ScannedImage>>

    suspend fun allIds(volume: String, folders: List<String>): Set<Long>
}
