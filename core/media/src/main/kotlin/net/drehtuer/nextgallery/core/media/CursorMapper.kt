package net.drehtuer.nextgallery.core.media

import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.ScannedImage

/** Maps rows of a cursor queried with [MediaStoreQuery.PROJECTION]; column indices are looked up once. */
class CursorMapper(cursor: Cursor, private val volumeName: String) {
    private val id = cursor.getColumnIndexOrThrow(Media._ID)
    private val bucketId = cursor.getColumnIndexOrThrow(Media.BUCKET_ID)
    private val bucketName = cursor.getColumnIndexOrThrow(Media.BUCKET_DISPLAY_NAME)
    private val relativePath = cursor.getColumnIndexOrThrow(Media.RELATIVE_PATH)
    private val displayName = cursor.getColumnIndexOrThrow(Media.DISPLAY_NAME)
    private val mimeType = cursor.getColumnIndexOrThrow(Media.MIME_TYPE)
    private val size = cursor.getColumnIndexOrThrow(Media.SIZE)
    private val width = cursor.getColumnIndexOrThrow(Media.WIDTH)
    private val height = cursor.getColumnIndexOrThrow(Media.HEIGHT)
    private val orientation = cursor.getColumnIndexOrThrow(Media.ORIENTATION)
    private val dateTaken = cursor.getColumnIndexOrThrow(Media.DATE_TAKEN)
    private val dateModified = cursor.getColumnIndexOrThrow(Media.DATE_MODIFIED)
    private val generationModified = cursor.getColumnIndexOrThrow(Media.GENERATION_MODIFIED)
    private val baseUri = MediaStore.Images.Media.getContentUri(volumeName)

    fun map(cursor: Cursor): ScannedImage {
        val rowId = cursor.getLong(id)
        return ScannedImage(
            id = MediaId(rowId),
            volumeName = volumeName,
            contentUri = ContentUris.withAppendedId(baseUri, rowId).toString(),
            bucketId = cursor.getLong(bucketId),
            bucketName = cursor.getString(bucketName).orEmpty(),
            relativePath = cursor.getString(relativePath).orEmpty(),
            displayName = cursor.getString(displayName).orEmpty(),
            mimeType = cursor.getString(mimeType).orEmpty(),
            sizeBytes = cursor.getLong(size),
            width = cursor.getInt(width),
            height = cursor.getInt(height),
            orientation = cursor.getInt(orientation),
            dateTakenMillis = if (cursor.isNull(dateTaken)) null else cursor.getLong(dateTaken),
            dateModifiedSeconds = cursor.getLong(dateModified),
            generationModified = cursor.getLong(generationModified),
        )
    }
}
