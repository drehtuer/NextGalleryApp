package net.drehtuer.nextgallery.core.media

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.common.deleteIfPossible
import net.drehtuer.nextgallery.core.data.edit.EncodedImage
import net.drehtuer.nextgallery.core.data.edit.MediaWriteSink
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import java.io.IOException
import javax.inject.Inject

class MediaStoreWriter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : MediaWriteSink {
    private val resolver get() = context.contentResolver

    override suspend fun existingNames(volumeName: String, relativePath: String): Set<String> =
        withContext(dispatchers.io) {
            val names = mutableSetOf<String>()
            resolver.query(
                MediaStore.Images.Media.getContentUri(volumeName),
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                "${MediaStore.Images.Media.RELATIVE_PATH} = ?",
                arrayOf(relativePath),
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) names += cursor.getString(0)
            }
            names
        }

    override suspend fun insertPending(
        volumeName: String,
        relativePath: String,
        displayName: String,
        mimeType: String,
    ): String = withContext(dispatchers.io) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(volumeName), values)
            ?: throw IOException("MediaStore did not create an entry")
        uri.toString()
    }

    override suspend fun write(contentUri: String, image: EncodedImage) = withContext(dispatchers.io) {
        val output = resolver.openOutputStream(contentUri.toUri(), "wt")
            ?: throw IOException("Cannot open the entry for writing")
        output.use { stream -> image.file.inputStream().use { it.copyTo(stream) } }
        Unit
    }

    override suspend fun publish(contentUri: String): MediaId = withContext(dispatchers.io) {
        val uri = contentUri.toUri()
        val values = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        if (resolver.update(uri, values, null, null) == 0) throw IOException("Cannot publish the entry")
        MediaId(ContentUris.parseId(uri))
    }

    override suspend fun discard(contentUri: String) = withContext(dispatchers.io) {
        resolver.delete(contentUri.toUri(), null, null)
        Unit
    }

    override suspend fun requestOverwrite(contentUri: String): PlatformConfirmation = withContext(dispatchers.io) {
        PlatformConfirmation(MediaStore.createWriteRequest(resolver, listOf(contentUri.toUri())).intentSender)
    }

    override suspend fun backup(contentUri: String): EncodedImage = withContext(dispatchers.io) {
        val input = resolver.openInputStream(contentUri.toUri()) ?: throw IOException("Cannot read the image")
        val file = EditorFiles.newTemporaryFile(context, "backup")
        try {
            input.use { stream -> file.outputStream().use { stream.copyTo(it) } }
        } catch (e: IOException) {
            file.deleteIfPossible()
            throw e
        }
        EncodedImage(file)
    }

    override suspend fun cleanupPending(olderThanMillis: Long) = withContext(dispatchers.io) {
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        pendingEntries(collection, olderThanMillis).forEach { resolver.delete(it, null, null) }
        EditorFiles.deleteOlderThan(context, olderThanMillis)
    }

    /** An app only sees its own pending entries. */
    private fun pendingEntries(collection: Uri, olderThanMillis: Long): List<Uri> {
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_ONLY)
            putString(
                android.content.ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Images.Media.DATE_ADDED} < ?",
            )
            putStringArray(
                android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf((olderThanMillis / MILLIS_PER_SECOND).toString()),
            )
        }
        val entries = mutableListOf<Uri>()
        resolver.query(collection, arrayOf(MediaStore.Images.Media._ID), args, null)?.use { cursor ->
            while (cursor.moveToNext()) entries += ContentUris.withAppendedId(collection, cursor.getLong(0))
        }
        return entries
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
