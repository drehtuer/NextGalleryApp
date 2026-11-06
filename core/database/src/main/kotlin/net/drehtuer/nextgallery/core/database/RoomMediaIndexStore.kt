package net.drehtuer.nextgallery.core.database

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.model.Folder
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import javax.inject.Inject

class RoomMediaIndexStore @Inject constructor(private val mediaDao: MediaDao, private val folderDao: FolderDao) :
    MediaIndexStore {
    override suspend fun upsertAll(items: List<MediaItem>) = mediaDao.upsertAll(items.map { it.toEntity() })

    override suspend fun deleteByIds(ids: Collection<Long>) {
        ids.chunked(SQLITE_MAX_VARIABLES).forEach { mediaDao.deleteByIds(it) }
    }

    override suspend fun idsForVolume(volume: String): Set<Long> = mediaDao.idsForVolume(volume).toSet()

    override suspend fun getById(id: MediaId): MediaItem? = mediaDao.getById(id.value)?.toModel()

    override fun pagedByBucket(bucketId: Long, config: PagingConfig): Flow<PagingData<MediaItem>> =
        Pager(config) { mediaDao.pagingByBucket(bucketId) }.flow.map { page -> page.map { it.toModel() } }

    override suspend fun idsInBucket(bucketId: Long): List<MediaId> = mediaDao.idsInBucket(bucketId).map { MediaId(it) }

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeFolders().map { rows -> rows.map { it.toModel() } }

    internal companion object {
        /** Stays below SQLite's limit of bound variables per statement. */
        const val SQLITE_MAX_VARIABLES = 900
    }
}
