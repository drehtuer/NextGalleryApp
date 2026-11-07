package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteItemTest {
    @Test
    fun parentPath_isTheFolderOfThePath() {
        val item = RemoteItem(path = "/Photos/IMG_1.jpg", name = "IMG_1.jpg", isDirectory = false)

        assertEquals("/Photos", item.parentPath)
    }
}
