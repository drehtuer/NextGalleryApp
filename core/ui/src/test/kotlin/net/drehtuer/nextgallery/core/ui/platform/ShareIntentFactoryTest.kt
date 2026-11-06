package net.drehtuer.nextgallery.core.ui.platform

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.ShareSpec
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareIntentFactoryTest {
    private val one = ShareSpec(listOf("content://media/1"), "image/png")
    private val two = ShareSpec(listOf("content://media/1", "content://media/2"), "image/*")

    @Test
    fun create_oneImage_usesSend() {
        assertEquals(Intent.ACTION_SEND, ShareIntentFactory.create(one).action)
    }

    @Test
    fun create_severalImages_usesSendMultiple() {
        assertEquals(Intent.ACTION_SEND_MULTIPLE, ShareIntentFactory.create(two).action)
    }

    @Test
    fun create_usesMimeType() {
        assertEquals("image/png", ShareIntentFactory.create(one).type)
    }

    @Test
    fun create_grantsReadPermission() {
        val flags = ShareIntentFactory.create(one).flags

        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun create_severalImages_putsAllInStream() {
        val stream = ShareIntentFactory.create(two).getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)

        assertEquals(listOf("content://media/1", "content://media/2"), stream?.map { it.toString() })
    }

    @Test
    fun create_severalImages_putsAllInClipData() {
        assertEquals(2, ShareIntentFactory.create(two).clipData?.itemCount)
    }
}
