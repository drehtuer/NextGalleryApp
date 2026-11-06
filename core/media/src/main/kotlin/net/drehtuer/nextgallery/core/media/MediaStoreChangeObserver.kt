package net.drehtuer.nextgallery.core.media

import android.content.Context
import android.database.ContentObserver
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.drehtuer.nextgallery.core.data.index.MediaChangeSource
import javax.inject.Inject

class MediaStoreChangeObserver @Inject constructor(@param:ApplicationContext private val context: Context) :
    MediaChangeSource {
    override fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.registerContentObserver(uri, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
}
