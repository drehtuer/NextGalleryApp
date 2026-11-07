package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.model.NextcloudAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemotePathMapperTest {
    private val account = NextcloudAccount("https://cloud.example.com/nc", "anna", "anna id", "secret")

    @Test
    fun davUrl_root_endsAtUserFolder() {
        assertEquals(
            "https://cloud.example.com/nc/remote.php/dav/files/anna%20id",
            RemotePathMapper.davUrl(account, "/").toString(),
        )
    }

    @Test
    fun davUrl_encodesEachSegment() {
        assertEquals(
            "https://cloud.example.com/nc/remote.php/dav/files/anna%20id/My%20Photos/a%23b%3F.jpg",
            RemotePathMapper.davUrl(account, "/My Photos/a#b?.jpg").toString(),
        )
    }

    @Test
    fun davUrl_dotDotSegment_isRefused() {
        assertThrows(IllegalArgumentException::class.java) { RemotePathMapper.davUrl(account, "/Photos/../../other") }
    }

    @Test
    fun previewUrl_hasFileIdAndSize() {
        assertEquals(
            "https://cloud.example.com/nc/index.php/core/preview?fileId=42&x=256&y=256&a=1",
            RemotePathMapper.previewUrl(account, 42, 256).toString(),
        )
    }

    @Test
    fun trashItemUrl_isBelowTheUsersTrash() {
        assertEquals(
            "https://cloud.example.com/nc/remote.php/dav/trashbin/anna%20id/trash/IMG_1.jpg.d123",
            RemotePathMapper.trashItemUrl(account, "IMG_1.jpg.d123").toString(),
        )
    }

    @Test
    fun trashItemUrl_dotDotName_isRefused() {
        assertThrows(IllegalArgumentException::class.java) { RemotePathMapper.trashItemUrl(account, "..") }
    }

    @Test
    fun restoreUrl_isBelowTheUsersRestoreFolder() {
        assertEquals(
            "https://cloud.example.com/nc/remote.php/dav/trashbin/anna%20id/restore/IMG_1.jpg",
            RemotePathMapper.restoreUrl(account, "IMG_1.jpg").toString(),
        )
    }
}
