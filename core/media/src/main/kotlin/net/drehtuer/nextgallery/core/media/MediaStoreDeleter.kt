package net.drehtuer.nextgallery.core.media

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.media.MediaDeleteSource
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import javax.inject.Inject

class MediaStoreDeleter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : MediaDeleteSource {
    override suspend fun trashRequest(contentUris: List<String>): PlatformConfirmation = withContext(dispatchers.io) {
        MediaStore.createTrashRequest(context.contentResolver, contentUris.map { it.toUri() }, true).toConfirmation()
    }

    override suspend fun restoreRequest(contentUris: List<String>): PlatformConfirmation = withContext(dispatchers.io) {
        MediaStore.createTrashRequest(context.contentResolver, contentUris.map { it.toUri() }, false).toConfirmation()
    }

    override suspend fun deleteRequest(contentUris: List<String>): PlatformConfirmation = withContext(dispatchers.io) {
        MediaStore.createDeleteRequest(context.contentResolver, contentUris.map { it.toUri() }).toConfirmation()
    }

    private fun android.app.PendingIntent.toConfirmation() = PlatformConfirmation(intentSender)
}
