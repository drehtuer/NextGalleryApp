package net.drehtuer.nextgallery.core.media

import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.edit.EncodedImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The real MediaStore provider (ARCHITECTURE.md §13.1). It only touches entries it inserted itself, in a folder of
 * its own, and deletes them afterwards. Confirming the system's overwrite dialog stays a manual check.
 */
@RunWith(AndroidJUnit4::class)
class MediaStoreWriterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dispatchers = object : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
    }
    private val writer = MediaStoreWriter(context, dispatchers)
    private val inserted = mutableListOf<String>()
    private val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
    private val folder = "Pictures/NextGalleryTest/"

    private suspend fun insert(): String {
        val name = "nextgallery-write-test-${System.nanoTime()}.jpg"
        return writer.insertPending(volume, folder, name, "image/jpeg").also { inserted += it }
    }

    private fun encoded(): EncodedImage {
        val file = File.createTempFile("encoded", ".jpg", context.cacheDir)
        file.outputStream().use {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        return EncodedImage(file)
    }

    private fun isPublished(uri: String): Boolean {
        // A query of the collection, like a gallery's: a query by id also returns the app's own pending entries.
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.getContentUri(volume),
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media._ID} = ?",
            arrayOf(uri.toUri().lastPathSegment),
            null,
        )
        return cursor!!.use { it.moveToFirst() }
    }

    @After
    fun deleteInserted() {
        inserted.forEach { uri ->
            try {
                context.contentResolver.delete(uri.toUri(), null, null)
            } catch (_: SecurityException) {
                // MediaStore answers like this for a row the test already removed.
            }
        }
    }

    @Test
    fun insertPending_entryIsHiddenFromNormalQueries() = runBlocking {
        assertFalse(isPublished(insert()))
    }

    @Test
    fun publish_makesWrittenEntryVisible() = runBlocking {
        val uri = insert()

        writer.write(uri, encoded())
        writer.publish(uri)

        assertTrue(isPublished(uri))
    }

    @Test
    fun existingNames_findsPublishedEntry() = runBlocking {
        val uri = insert()
        writer.write(uri, encoded())
        writer.publish(uri)

        val name = context.contentResolver.query(
            uri.toUri(),
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
            null,
            null,
            null,
        )!!.use {
            it.moveToFirst()
            it.getString(0)
        }

        assertTrue(name in writer.existingNames(volume, folder))
    }

    @Test
    fun write_replacesContentOfOwnEntry() = runBlocking {
        val uri = insert()
        writer.write(uri, encoded())
        writer.publish(uri)
        val backup = writer.backup(uri)
        val replacement = encoded().apply { file.writeBytes(file.readBytes() + 0) }

        writer.write(uri, replacement)

        assertEquals(replacement.file.length(), writer.backup(uri).file.length())
        assertTrue(backup.file.length() < replacement.file.length())
    }

    @Test
    fun discard_removesPendingEntry() = runBlocking {
        val uri = insert()

        writer.discard(uri)

        val args = Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE) }
        val remaining = context.contentResolver.query(uri.toUri(), null, args, null)?.use { it.count }
        assertEquals(0, remaining)
    }

    @Test
    fun cleanupPending_removesOwnOldPendingEntry() = runBlocking {
        val uri = insert()

        writer.cleanupPending(System.currentTimeMillis() + FUTURE_MILLIS)

        val args = Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE) }
        val remaining = context.contentResolver.query(uri.toUri(), null, args, null)?.use { it.count }
        assertEquals(0, remaining)
    }

    @Test
    fun requestOverwrite_returnsIntentSender() = runBlocking {
        val uri = insert()
        writer.write(uri, encoded())
        writer.publish(uri)

        assertTrue(writer.requestOverwrite(uri).handle is IntentSender)
    }

    private companion object {
        const val FUTURE_MILLIS = 60_000L
    }
}
