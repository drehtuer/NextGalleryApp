package net.drehtuer.nextgallery.core.data.media

import kotlinx.coroutines.flow.Flow
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import javax.inject.Inject

interface MediaRepository {
    fun observeIndexedCount(): Flow<Int>

    suspend fun getItem(id: MediaId): MediaItem?
}

class DefaultMediaRepository @Inject constructor(private val indexStore: MediaIndexStore) : MediaRepository {
    override fun observeIndexedCount(): Flow<Int> = indexStore.observeCount()

    override suspend fun getItem(id: MediaId): MediaItem? = indexStore.getById(id)
}
