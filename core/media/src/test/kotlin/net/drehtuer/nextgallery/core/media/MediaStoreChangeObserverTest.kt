package net.drehtuer.nextgallery.core.media

import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MediaStoreChangeObserverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private fun registeredObservers() = shadowOf(context.contentResolver).getContentObservers(uri).size

    @Test
    fun changes_whileCollected_registersObserver() = runTest(UnconfinedTestDispatcher()) {
        val job = launch { MediaStoreChangeObserver(context).changes().collect {} }

        assertEquals(1, registeredObservers())
        job.cancel()
    }

    @Test
    fun changes_collectionCancelled_unregistersObserver() = runTest(UnconfinedTestDispatcher()) {
        val job = launch { MediaStoreChangeObserver(context).changes().collect {} }

        job.cancel()

        assertEquals(0, registeredObservers())
    }
}
