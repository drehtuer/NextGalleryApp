package net.drehtuer.nextgallery.core.media

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The system dialogs only exist on a device (ARCHITECTURE.md §13.1); confirming them stays a manual check. */
@RunWith(AndroidJUnit4::class)
class MediaStoreDeleterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dispatchers = object : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
    }
    private val deleter = MediaStoreDeleter(context, dispatchers)
    private lateinit var inserted: Uri

    @Before
    fun insertImage() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "nextgallery-delete-test-${System.nanoTime()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NextGalleryTest")
        }
        inserted = context.contentResolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values,
        )!!
        context.contentResolver.openOutputStream(inserted)!!.use {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
    }

    @After
    fun deleteImage() {
        context.contentResolver.delete(inserted, null, null)
    }

    @Test
    fun trashRequest_returnsIntentSender() = runBlocking {
        assertTrue(deleter.trashRequest(listOf(inserted.toString())).handle is IntentSender)
    }

    @Test
    fun restoreRequest_returnsIntentSender() = runBlocking {
        assertTrue(deleter.restoreRequest(listOf(inserted.toString())).handle is IntentSender)
    }

    @Test
    fun deleteRequest_returnsIntentSender() = runBlocking {
        assertTrue(deleter.deleteRequest(listOf(inserted.toString())).handle is IntentSender)
    }
}
