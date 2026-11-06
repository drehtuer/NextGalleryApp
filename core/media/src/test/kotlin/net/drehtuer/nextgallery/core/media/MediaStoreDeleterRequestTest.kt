package net.drehtuer.nextgallery.core.media

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
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
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Which request each action uses; `MediaStoreDeleterTest` checks on a device that the requests really work. */
@RunWith(AndroidJUnit4::class)
class MediaStoreDeleterRequestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri = "content://media/1"
    private val trashSender = mockk<IntentSender>()
    private val restoreSender = mockk<IntentSender>()
    private val deleteSender = mockk<IntentSender>()

    private fun pendingIntent(sender: IntentSender) = mockk<PendingIntent> { every { intentSender } returns sender }

    @Before
    fun setUp() {
        mockkStatic(MediaStore::class)
        val uris = listOf(uri.toUri())
        every { MediaStore.createTrashRequest(any(), uris, true) } returns pendingIntent(trashSender)
        every { MediaStore.createTrashRequest(any(), uris, false) } returns pendingIntent(restoreSender)
        every { MediaStore.createDeleteRequest(any(), uris) } returns pendingIntent(deleteSender)
    }

    @After
    fun tearDown() = unmockkStatic(MediaStore::class)

    private fun TestScope.deleter() = MediaStoreDeleter(
        context,
        object : DispatcherProvider {
            override val io = StandardTestDispatcher(testScheduler)
            override val default = io
        },
    )

    @Test
    fun trashRequest_movesToTrash() = runTest {
        assertSame(trashSender, deleter().trashRequest(listOf(uri)).handle)
    }

    @Test
    fun restoreRequest_takesOutOfTrash() = runTest {
        assertSame(restoreSender, deleter().restoreRequest(listOf(uri)).handle)
    }

    @Test
    fun deleteRequest_deletesPermanently() = runTest {
        assertSame(deleteSender, deleter().deleteRequest(listOf(uri)).handle)
    }
}
