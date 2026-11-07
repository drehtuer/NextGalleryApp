package net.drehtuer.nextgallery.core.data.media

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import net.drehtuer.nextgallery.core.common.RelativePaths
import net.drehtuer.nextgallery.core.data.index.IndexScheduler
import net.drehtuer.nextgallery.core.data.index.MediaChangeSource
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.index.MediaScanSource
import net.drehtuer.nextgallery.core.model.DeviceFolder
import net.drehtuer.nextgallery.core.model.Folder
import javax.inject.Inject

interface FolderRepository {
    fun observeFolders(): Flow<List<Folder>>

    /** Emits null when the folder has no indexed images. */
    fun observeFolder(bucketId: Long): Flow<Folder?>

    /**
     * The folders MediaStore has images in, regardless of the folder rules, sorted by path. Folders with a `.nomedia`
     * file are not among them, because MediaStore hides their images (D6). Images in the storage root are left out: a
     * rule on the root would also apply to every other folder.
     */
    fun observeAllDeviceFolders(): Flow<List<DeviceFolder>>
}

class DefaultFolderRepository @Inject constructor(
    private val indexStore: MediaIndexStore,
    private val scanSource: MediaScanSource,
    private val mediaChangeSource: MediaChangeSource,
) : FolderRepository {
    override fun observeFolders(): Flow<List<Folder>> = indexStore.observeFolders()

    override fun observeFolder(bucketId: Long): Flow<Folder?> =
        indexStore.observeFolders().map { folders -> folders.find { it.bucketId == bucketId } }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun observeAllDeviceFolders(): Flow<List<DeviceFolder>> = mediaChangeSource.changes()
        .debounce(IndexScheduler.CHANGE_DEBOUNCE_MILLIS)
        .onStart { emit(Unit) }
        .mapLatest { scanDeviceFolders() }

    private suspend fun scanDeviceFolders(): List<DeviceFolder> {
        val folders = HashMap<String, DeviceFolder>()
        scanSource.volumes().forEach { volume ->
            scanSource.fullScan(volume).collect { batch ->
                batch.groupingBy { RelativePaths.normalize(it.relativePath) }.eachCount().forEach { (path, count) ->
                    if (path.isNotEmpty()) folders.addImages(path, count)
                }
            }
        }
        return folders.values.sortedBy { it.relativePath.lowercase() }
    }

    /** Paths are compared without case, because shared storage is case-insensitive. */
    private fun MutableMap<String, DeviceFolder>.addImages(path: String, count: Int) {
        val folder = getOrPut(path.lowercase()) { DeviceFolder(path.substringAfterLast('/'), path, 0) }
        put(path.lowercase(), folder.copy(itemCount = folder.itemCount + count))
    }
}
