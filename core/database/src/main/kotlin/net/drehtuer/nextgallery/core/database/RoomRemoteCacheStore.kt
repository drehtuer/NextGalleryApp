package net.drehtuer.nextgallery.core.database

import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteCacheStore
import net.drehtuer.nextgallery.core.model.RemoteItem
import javax.inject.Inject

class RoomRemoteCacheStore @Inject constructor(private val dao: RemoteCacheDao, private val clock: Clock) :
    RemoteCacheStore {
    /** In the order the listing was stored in: the repository sorts before storing, and this keeps its sorting. */
    override suspend fun get(parentPath: String): List<RemoteItem> =
        dao.getFolder(parentPath).sortedWith(CACHE_ORDER).map { it.toModel() }

    override suspend fun replaceFolder(parentPath: String, items: List<RemoteItem>) {
        val now = clock.nowMillis()
        dao.replaceFolder(parentPath, items.map { it.toEntity(now) })
    }

    override suspend fun clear() = dao.clear()

    private companion object {
        /** Folders by name, then images newest first, as `RemoteBrowseRepository` shows them. */
        val CACHE_ORDER = compareByDescending<RemoteCacheEntity> { it.isDirectory }
            .thenBy { if (it.isDirectory) it.name.lowercase() else "" }
            .thenByDescending { it.lastModifiedMillis }
    }
}
