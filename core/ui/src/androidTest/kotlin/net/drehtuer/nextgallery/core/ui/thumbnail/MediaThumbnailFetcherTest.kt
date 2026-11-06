package net.drehtuer.nextgallery.core.ui.thumbnail

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Size
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against the real system thumbnail cache (ARCHITECTURE.md §13.1); the test app owns the image it inserts. */
@RunWith(AndroidJUnit4::class)
class MediaThumbnailFetcherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var inserted: Uri

    @Before
    fun insertImage() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "nextgallery-thumbnail-test-${System.nanoTime()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NextGalleryTest")
        }
        inserted = context.contentResolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values,
        )!!
        context.contentResolver.openOutputStream(inserted)!!.use { output ->
            Bitmap.createBitmap(1_024, 1_024, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
    }

    @After
    fun deleteImage() {
        context.contentResolver.delete(inserted, null, null)
    }

    @Test
    fun fetch_returnsImageNoLargerThanRequested() = runBlocking {
        val fetcher = MediaThumbnailFetcher(
            data = MediaThumbnail(id = 1, contentUri = inserted.toString(), dateModifiedSeconds = 1),
            options = Options(context, size = Size(256, 256)),
            contentResolver = context.contentResolver,
        )

        val result = fetcher.fetch() as ImageFetchResult

        assertTrue(result.image.width <= 256)
    }
}
