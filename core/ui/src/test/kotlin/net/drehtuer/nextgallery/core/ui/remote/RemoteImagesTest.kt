package net.drehtuer.nextgallery.core.ui.remote

import net.drehtuer.nextgallery.core.model.RemoteItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RemoteImagesTest {
    private val item = RemoteItem("/a.jpg", "a.jpg", false, etag = "e1", fileId = 42)

    @Test
    fun cacheKey_containsFileIdEtagAndSize() {
        assertEquals("remote:42:e1:256", remoteImageCacheKey(item, 256))
    }

    @Test
    fun cacheKey_changedEtag_isDifferent() {
        assertNotEquals(remoteImageCacheKey(item, 256), remoteImageCacheKey(item.copy(etag = "e2"), 256))
    }

    @Test
    fun cacheKey_otherSize_isDifferent() {
        assertNotEquals(remoteImageCacheKey(item, 256), remoteImageCacheKey(item, 512))
    }

    @Test
    fun cacheKey_original_isDifferentFromPreviews() {
        assertEquals("remote:42:e1:original", remoteImageCacheKey(item, null))
    }

    @Test
    fun cacheKey_withoutFileId_usesThePath() {
        assertEquals("remote:/a.jpg::256", remoteImageCacheKey(RemoteItem("/a.jpg", "a.jpg", false), 256))
    }
}
