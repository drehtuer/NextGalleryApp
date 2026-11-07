package net.drehtuer.nextgallery.core.ui.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.drehtuer.nextgallery.core.model.ShareSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareIntentFactoryTest {
    private val one = ShareSpec(listOf("content://media/1"), "image/png")
    private val two = ShareSpec(listOf("content://media/1", "content://media/2"), "image/*")

    @Test
    fun shareImages_opensChooser() {
        val context = mockk<Context>(relaxed = true)

        val started = context.shareImages(one)

        assertTrue(started)
        verify { context.startActivity(match { it.action == Intent.ACTION_CHOOSER }) }
    }

    @Test
    fun shareImages_systemRefusesTheIntent_saysSo() {
        val context = mockk<Context> { every { startActivity(any()) } throws RuntimeException("Failure from system") }

        assertFalse(context.shareImages(one))
    }

    private val toNextcloud = one.copy(targetPackage = "com.nextcloud.client")

    @Test
    fun create_targetPackage_isExplicit() {
        assertEquals("com.nextcloud.client", ShareIntentFactory.create(toNextcloud).`package`)
    }

    @Test
    fun create_noTargetPackage_isNotExplicit() {
        assertNull(ShareIntentFactory.create(one).`package`)
    }

    @Test
    fun handOffImages_opensTheTargetAppWithoutChooser() {
        val context = mockk<Context>(relaxed = true)

        val start = context.handOffImages(toNextcloud)

        assertEquals(HandoffStart.STARTED, start)
        verify {
            context.startActivity(
                match { it.action == Intent.ACTION_SEND && it.`package` == "com.nextcloud.client" },
            )
        }
    }

    @Test
    fun handOffImages_appGone_saysSo() {
        val context = mockk<Context> { every { startActivity(any()) } throws ActivityNotFoundException() }

        assertEquals(HandoffStart.APP_MISSING, context.handOffImages(toNextcloud))
    }

    @Test
    fun handOffImages_systemRefusesTheIntent_saysSo() {
        val context = mockk<Context> { every { startActivity(any()) } throws RuntimeException("Failure from system") }

        assertEquals(HandoffStart.FAILED, context.handOffImages(toNextcloud))
    }

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
