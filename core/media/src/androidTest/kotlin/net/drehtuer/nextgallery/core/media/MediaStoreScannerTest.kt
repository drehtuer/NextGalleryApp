package net.drehtuer.nextgallery.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Runs against the real MediaStore (ARCHITECTURE.md §13.1); the test app owns the image it inserts. */
@RunWith(AndroidJUnit4::class)
class MediaStoreScannerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dispatchers = object : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
    }
    private val scanner = MediaStoreScanner(context, dispatchers)
    private val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
    private var generationBeforeInsert = 0L
    private lateinit var inserted: Uri

    @Before
    fun insertImage(): Unit = runBlocking {
        generationBeforeInsert = scanner.currentGeneration(volume)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "nextgallery-test-${System.nanoTime()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NextGalleryTest")
        }
        inserted = context.contentResolver.insert(MediaStore.Images.Media.getContentUri(volume), values)!!
        context.contentResolver.openOutputStream(inserted)!!.use {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
    }

    @After
    fun deleteImage() {
        context.contentResolver.delete(inserted, null, null)
    }

    private val insertedId get() = inserted.lastPathSegment!!.toLong()
    private val testFolder = listOf("Pictures/NextGalleryTest")
    private val otherFolder = listOf("Pictures/NextGalleryTestOther")

    @Test
    fun fullScan_containsInsertedImage() = runBlocking {
        val ids = scanner.fullScan(volume, testFolder).toList().flatten().map { it.id.value }

        assertTrue(insertedId in ids)
    }

    @Test
    fun fullScan_otherFolder_doesNotContainInsertedImage() = runBlocking {
        val ids = scanner.fullScan(volume, otherFolder).toList().flatten().map { it.id.value }

        assertFalse(insertedId in ids)
    }

    @Test
    fun fullScan_parentFolder_containsInsertedImage() = runBlocking {
        val ids = scanner.fullScan(volume, listOf("Pictures")).toList().flatten().map { it.id.value }

        assertTrue(insertedId in ids)
    }

    @Test
    fun changesSince_generationBeforeInsert_containsInsertedImage() = runBlocking {
        val ids = scanner.changesSince(volume, generationBeforeInsert, testFolder)
            .toList()
            .flatten()
            .map { it.id.value }

        assertTrue(insertedId in ids)
    }

    @Test
    fun allIds_containsInsertedImage() = runBlocking {
        assertTrue(insertedId in scanner.allIds(volume, testFolder))
    }

    @Test
    fun allIds_otherFolder_doesNotContainInsertedImage() = runBlocking {
        assertFalse(insertedId in scanner.allIds(volume, otherFolder))
    }

    /** MediaStore applies `.nomedia` when it scans, so this needs a real file on disk and a real scan (D6). */
    @Test
    fun fullScan_folderWithNoMediaFile_hidesImage() = runBlocking {
        val marker = insertFile(".nomedia", "application/octet-stream", "Download/NextGalleryTestHidden")
        val image = insertFile("hidden.jpg", "image/jpeg", "Download/NextGalleryTestHidden")
        try {
            context.contentResolver.openOutputStream(marker)!!.use { it.write(0) }
            context.contentResolver.openOutputStream(image)!!.use {
                Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            val imagePath = pathOf(image)
            scan(pathOf(marker))
            scan(imagePath)
            scan(imagePath.substringBeforeLast('/'))

            val ids = scanner.fullScan(volume, listOf("Download/NextGalleryTestHidden"))
                .toList()
                .flatten()
                .map { it.id.value }

            assertFalse(image.lastPathSegment!!.toLong() in ids)
        } finally {
            context.contentResolver.delete(image, null, null)
            context.contentResolver.delete(marker, null, null)
        }
    }

    private fun pathOf(uri: Uri): String = context.contentResolver
        .query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)!!
        .use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }

    private suspend fun scan(path: String) = suspendCoroutine { continuation ->
        MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ -> continuation.resume(Unit) }
    }

    private fun insertFile(name: String, mimeType: String, relativePath: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        return context.contentResolver.insert(MediaStore.Files.getContentUri(volume), values)!!
    }

    @Test
    fun volumes_containsPrimaryVolume() = runBlocking {
        assertTrue(volume in scanner.volumes())
    }

    @Test
    fun mediaStoreVersion_isNotBlank() = runBlocking {
        assertTrue(scanner.mediaStoreVersion(volume).isNotBlank())
    }
}
