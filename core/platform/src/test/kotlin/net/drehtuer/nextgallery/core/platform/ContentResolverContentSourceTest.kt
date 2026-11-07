package net.drehtuer.nextgallery.core.platform

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
class ContentResolverContentSourceTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val uri = Uri.parse("content://media/external/images/media/1")

    @Test
    fun open_readsTheOriginalBytes() {
        val original = MediaStore.setRequireOriginal(uri)
        shadowOf(application.contentResolver).registerInputStream(original, ByteArrayInputStream("abc".toByteArray()))

        val bytes = ContentResolverContentSource(application).open(uri.toString())?.use { it.readBytes() }

        assertEquals("abc", bytes?.decodeToString())
    }
}
