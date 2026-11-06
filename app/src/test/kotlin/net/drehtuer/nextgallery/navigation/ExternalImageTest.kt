package net.drehtuer.nextgallery.navigation

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Plain Application: this test needs no Hilt components.
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ExternalImageTest {
    private fun view(uri: String, type: String?) = Intent(Intent.ACTION_VIEW).setDataAndType(uri.toUri(), type)

    @Test
    fun externalImageUri_viewOfContentImage_returnsUri() {
        assertEquals("content://files/1", view("content://files/1", "image/jpeg").externalImageUri())
    }

    @Test
    fun externalImageUri_launcherStart_returnsNull() {
        assertNull(Intent(Intent.ACTION_MAIN).externalImageUri())
    }

    @Test
    fun externalImageUri_fileUri_returnsNull() {
        assertNull(view("file:///sdcard/1.jpg", "image/jpeg").externalImageUri())
    }

    @Test
    fun externalImageUri_otherType_returnsNull() {
        assertNull(view("content://files/1", "video/mp4").externalImageUri())
    }

    @Test
    fun externalImageUri_otherAction_returnsNull() {
        val intent = view("content://files/1", "image/jpeg").setAction(Intent.ACTION_EDIT)

        assertNull(intent.externalImageUri())
    }
}
