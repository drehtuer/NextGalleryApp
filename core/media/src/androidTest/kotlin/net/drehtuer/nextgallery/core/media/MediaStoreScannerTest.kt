package net.drehtuer.nextgallery.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
    fun insertImage() = runBlocking {
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

    @Test
    fun fullScan_containsInsertedImage() = runBlocking {
        val ids = scanner.fullScan(volume).toList().flatten().map { it.id.value }

        assertTrue(insertedId in ids)
    }

    @Test
    fun changesSince_generationBeforeInsert_containsInsertedImage() = runBlocking {
        val ids = scanner.changesSince(volume, generationBeforeInsert).toList().flatten().map { it.id.value }

        assertTrue(insertedId in ids)
    }

    @Test
    fun allIds_containsInsertedImage() = runBlocking {
        assertTrue(insertedId in scanner.allIds(volume))
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
