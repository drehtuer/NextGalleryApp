package net.drehtuer.nextgallery.core.media

import android.app.PendingIntent
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.edit.EncodedImage
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import java.io.File
import java.io.IOException

/**
 * What the writer asks of MediaStore, against a stand-in provider: nothing here reaches a real gallery.
 * `MediaStoreWriterTest` checks on a device that the real provider accepts it (ARCHITECTURE.md §13.1).
 */
@RunWith(AndroidJUnit4::class)
class MediaStoreWriterRequestTest {
    /** One row per URI, with the values it was given; files live in the cache directory. */
    class FakeMediaProvider : ContentProvider() {
        override fun onCreate() = true

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            if (refuseInsert) return null
            val row = ContentUris.withAppendedId(uri, nextId++)
            rows[row.toString()] = ContentValues(values)
            return row
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int {
            val row = rows[uri.toString()] ?: return 0
            row.putAll(values)
            return 1
        }

        /** By id, because like in MediaStore an entry can be addressed through several volume names. */
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            val key = rows.keys.firstOrNull { ContentUris.parseId(it.toUri()) == ContentUris.parseId(uri) }
            return if (key != null && rows.remove(key) != null) 1 else 0
        }

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            queryArgs: Bundle?,
            cancellationSignal: CancellationSignal?,
        ): Cursor {
            lastQueryArgs = queryArgs
            val cursor = MatrixCursor(projection)
            val args = queryArgs?.getStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
            if (queryArgs?.containsKey(MediaStore.QUERY_ARG_MATCH_PENDING) == true) {
                rows.filter { (_, values) -> values.getAsInteger(MediaStore.Images.Media.IS_PENDING) == 1 }
                    .filter { (_, values) -> values.getAsLong(MediaStore.Images.Media.DATE_ADDED) < args!![0].toLong() }
                    .forEach { (key, _) -> cursor.addRow(arrayOf(ContentUris.parseId(key.toUri()))) }
            } else {
                rows.values
                    .filter { it.getAsString(MediaStore.Images.Media.RELATIVE_PATH) == args?.get(0) }
                    .forEach { cursor.addRow(arrayOf(it.getAsString(MediaStore.Images.Media.DISPLAY_NAME))) }
            }
            return cursor
        }

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = query(
            uri,
            projection,
            Bundle().apply {
                putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            },
            null,
        )

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            openedMode = mode
            val file = File(context!!.cacheDir, "row-${ContentUris.parseId(uri)}")
            if (!file.exists() && mode == "r") throw java.io.FileNotFoundException()
            file.createNewFile()
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY

    @Before
    fun setUp() {
        rows.clear()
        refuseInsert = false
        lastQueryArgs = null
        openedMode = null
        Robolectric.setupContentProvider(FakeMediaProvider::class.java, MediaStore.AUTHORITY)
    }

    private fun TestScope.writer() = MediaStoreWriter(
        context,
        object : DispatcherProvider {
            override val io = StandardTestDispatcher(testScheduler)
            override val default = io
        },
    )

    private fun encoded(text: String) = EncodedImage(
        File.createTempFile("encoded", null, context.cacheDir).apply {
            writeText(text)
        },
    )

    private fun fileOfRow(uri: String) = File(context.cacheDir, "row-${ContentUris.parseId(uri.toUri())}")

    private suspend fun MediaStoreWriter.insert(name: String = "a.jpg") =
        insertPending(volume, "DCIM/Camera/", name, "image/jpeg")

    @Test
    fun insertPending_setsIsPending() = runTest {
        val uri = writer().insert()

        assertEquals(1, rows[uri]?.getAsInteger(MediaStore.Images.Media.IS_PENDING))
    }

    @Test
    fun insertPending_setsNameFolderAndType() = runTest {
        val uri = writer().insert()

        val values = rows.getValue(uri)
        assertEquals(
            listOf("a.jpg", "DCIM/Camera/", "image/jpeg"),
            listOf(
                values.getAsString(MediaStore.Images.Media.DISPLAY_NAME),
                values.getAsString(MediaStore.Images.Media.RELATIVE_PATH),
                values.getAsString(MediaStore.Images.Media.MIME_TYPE),
            ),
        )
    }

    @Test
    fun insertPending_usesTheVolumeOfTheSource() = runTest {
        val uri = writer().insert()

        assertTrue(uri.startsWith(MediaStore.Images.Media.getContentUri(volume).toString()))
    }

    @Test(expected = IOException::class)
    fun insertPending_refused_throwsIoException() = runTest {
        refuseInsert = true

        writer().insert()
    }

    @Test
    fun write_replacesContent() = runTest {
        val writer = writer()
        val uri = writer.insert()
        fileOfRow(uri).writeText("much longer old content")

        writer.write(uri, encoded("new"))

        assertEquals("new", fileOfRow(uri).readText())
    }

    @Test
    fun write_opensForTruncatingWrite() = runTest {
        val writer = writer()
        val uri = writer.insert()

        writer.write(uri, encoded("new"))

        assertEquals("wt", openedMode)
    }

    @Test
    fun publish_clearsIsPending() = runTest {
        val writer = writer()
        val uri = writer.insert()

        writer.publish(uri)

        assertEquals(0, rows[uri]?.getAsInteger(MediaStore.Images.Media.IS_PENDING))
    }

    @Test
    fun publish_returnsIdOfTheEntry() = runTest {
        val writer = writer()
        val uri = writer.insert()

        assertEquals(MediaId(ContentUris.parseId(uri.toUri())), writer.publish(uri))
    }

    @Test(expected = IOException::class)
    fun publish_entryIsGone_throwsIoException() = runTest {
        writer().publish(MediaStore.Images.Media.getContentUri(volume).toString() + "/12345")
    }

    @Test
    fun discard_removesTheEntry() = runTest {
        val writer = writer()
        val uri = writer.insert()

        writer.discard(uri)

        assertFalse(uri in rows)
    }

    @Test
    fun existingNames_listsOnlyTheFolder() = runTest {
        val writer = writer()
        writer.insert("a.jpg")
        writer.insertPending(volume, "Pictures/", "b.jpg", "image/jpeg")

        assertEquals(setOf("a.jpg"), writer.existingNames(volume, "DCIM/Camera/"))
    }

    @Test
    fun backup_copiesCurrentContent() = runTest {
        val writer = writer()
        val uri = writer.insert()
        fileOfRow(uri).writeText("original")

        assertEquals("original", writer.backup(uri).file.readText())
    }

    @Test
    fun backup_opensForReadingOnly() = runTest {
        val writer = writer()
        val uri = writer.insert()
        fileOfRow(uri).writeText("original")

        writer.backup(uri)

        assertEquals("r", openedMode)
    }

    @Test
    fun requestOverwrite_buildsRequestForTheUri() = runTest {
        val sender = mockk<IntentSender>()
        val uri = "content://media/external_primary/images/media/5"
        mockkStatic(MediaStore::class)
        try {
            every { MediaStore.createWriteRequest(any(), listOf(uri.toUri())) } returns
                mockk<PendingIntent> { every { intentSender } returns sender }

            assertSame(sender, writer().requestOverwrite(uri).handle)
        } finally {
            unmockkStatic(MediaStore::class)
        }
    }

    @Test
    fun cleanupPending_deletesOldPendingEntries() = runTest {
        val writer = writer()
        val uri = writer.insert()
        rows.getValue(uri).put(MediaStore.Images.Media.DATE_ADDED, 10L)

        writer.cleanupPending(olderThanMillis = 20_000)

        assertFalse(uri in rows)
    }

    @Test
    fun cleanupPending_keepsNewerPendingEntries() = runTest {
        val writer = writer()
        val uri = writer.insert()
        rows.getValue(uri).put(MediaStore.Images.Media.DATE_ADDED, 30L)

        writer.cleanupPending(olderThanMillis = 20_000)

        assertTrue(uri in rows)
    }

    @Test
    fun cleanupPending_keepsPublishedEntries() = runTest {
        val writer = writer()
        val uri = writer.insert()
        rows.getValue(uri).put(MediaStore.Images.Media.DATE_ADDED, 10L)
        writer.publish(uri)

        writer.cleanupPending(olderThanMillis = 20_000)

        assertTrue(uri in rows)
    }

    @Test
    fun cleanupPending_asksOnlyForPendingEntries() = runTest {
        writer().cleanupPending(olderThanMillis = 20_000)

        assertEquals(MediaStore.MATCH_ONLY, lastQueryArgs?.getInt(MediaStore.QUERY_ARG_MATCH_PENDING))
    }

    @Test
    fun cleanupPending_deletesOldTemporaryFiles() = runTest {
        val old = EditorFiles.newTemporaryFile(context, "encoded").apply { setLastModified(1_000) }

        writer().cleanupPending(olderThanMillis = 20_000)

        assertFalse(old.exists())
    }

    @Test
    fun cleanupPending_keepsNewerTemporaryFiles() = runTest {
        val recent = EditorFiles.newTemporaryFile(context, "encoded").apply { setLastModified(30_000) }

        writer().cleanupPending(olderThanMillis = 20_000)

        assertTrue(recent.exists())
    }

    private companion object {
        val rows = mutableMapOf<String, ContentValues>()
        var nextId = 1L
        var refuseInsert = false
        var lastQueryArgs: Bundle? = null
        var openedMode: String? = null
    }
}
