package net.drehtuer.nextgallery.core.media

import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.index.MediaScanSource
import net.drehtuer.nextgallery.core.model.ScannedImage
import javax.inject.Inject

class MediaStoreScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : MediaScanSource {
    override suspend fun volumes(): Set<String> = withContext(dispatchers.io) {
        MediaStore.getExternalVolumeNames(context)
    }

    override suspend fun mediaStoreVersion(volume: String): String = withContext(dispatchers.io) {
        MediaStore.getVersion(context, volume)
    }

    override suspend fun currentGeneration(volume: String): Long = withContext(dispatchers.io) {
        MediaStore.getGeneration(context, volume)
    }

    override fun fullScan(volume: String, folders: List<String>): Flow<List<ScannedImage>> =
        query(volume, MediaStoreQuery.imagesIn(folders))

    override fun changesSince(volume: String, generation: Long, folders: List<String>): Flow<List<ScannedImage>> =
        query(volume, MediaStoreQuery.changedSince(generation, folders))

    override suspend fun allIds(volume: String, folders: List<String>): Set<Long> = withContext(dispatchers.io) {
        val selection = MediaStoreQuery.imagesIn(folders)
        context.contentResolver.query(
            MediaStore.Images.Media.getContentUri(volume),
            arrayOf(MediaStore.Images.Media._ID),
            selection.sql,
            selection.args,
            null,
        )?.use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getLong(0)) } }.orEmpty()
    }

    private fun query(volume: String, selection: Selection): Flow<List<ScannedImage>> = flow {
        context.contentResolver.query(
            MediaStore.Images.Media.getContentUri(volume),
            MediaStoreQuery.PROJECTION,
            selection.sql,
            selection.args,
            MediaStoreQuery.SORT_ORDER,
        )?.use { cursor ->
            val mapper = CursorMapper(cursor, volume)
            val batch = ArrayList<ScannedImage>(BATCH_SIZE)
            while (cursor.moveToNext()) {
                batch += mapper.map(cursor)
                if (batch.size == BATCH_SIZE) {
                    emit(batch.toList())
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) emit(batch.toList())
        }
    }.flowOn(dispatchers.io)

    private companion object {
        const val BATCH_SIZE = 500
    }
}
