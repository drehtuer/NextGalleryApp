package net.drehtuer.nextgallery.core.platform

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileProviderShareCacheTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val cache = FileProviderShareCache(application)

    /** FileProvider keeps its paths in a static map, but each Robolectric test has another cache folder. */
    @Before
    fun forgetPathsOfEarlierTests() {
        val paths = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (paths.get(null) as MutableMap<*, *>).clear()
    }

    @Test
    fun newFile_isInsideTheTemporaryFolder() {
        val file = cache.newFile(0, "a.jpg")

        assertTrue(file.canonicalPath.startsWith(File(application.cacheDir, "remote_share").canonicalPath))
    }

    @Test
    fun newFile_equalNames_getDifferentFiles() {
        assertFalse(cache.newFile(0, "a.jpg") == cache.newFile(1, "a.jpg"))
    }

    @Test
    fun clear_removesTheFiles() {
        val file = cache.newFile(0, "a.jpg").apply { writeText("x") }

        cache.clear()

        assertFalse(file.exists())
    }

    @Test
    fun contentUri_isAContentUriOfTheProvider() {
        val file = cache.newFile(0, "a b.jpg").apply { writeText("x") }

        val uri = Uri.parse(cache.contentUri(file))

        assertEquals("content", uri.scheme)
        assertEquals("${application.packageName}.remoteshare", uri.authority)
        assertEquals("/remote_share/0/a b.jpg", uri.path)
    }

    @Test
    fun contentUri_fileOutsideTheFolder_isRefused() {
        val outside = File(application.filesDir, "credentials").apply {
            parentFile?.mkdirs()
            writeText("secret")
        }

        assertThrows(IllegalArgumentException::class.java) { cache.contentUri(outside) }
    }

    @Test
    fun contentUri_otherFileInTheCache_isRefused() {
        val outside = File(application.cacheDir, "image_cache/x").apply {
            parentFile?.mkdirs()
            writeText("x")
        }

        assertThrows(IllegalArgumentException::class.java) { cache.contentUri(outside) }
    }
}
