package net.drehtuer.nextgallery.core.ui.thumbnail

import android.content.Context
import coil3.request.Options
import coil3.size.Size
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaThumbnailKeyerTest {
    private val keyer = MediaThumbnailKeyer()
    private val thumbnail = MediaThumbnail(id = 1, contentUri = "content://media/1", dateModifiedSeconds = 100)

    private fun options(side: Int = 256) = Options(mockk<Context>(), size = Size(side, side))

    @Test
    fun key_sameInputs_isStable() {
        assertEquals(keyer.key(thumbnail, options()), keyer.key(thumbnail, options()))
    }

    @Test
    fun key_otherImage_differs() {
        assertNotEquals(keyer.key(thumbnail, options()), keyer.key(thumbnail.copy(id = 2), options()))
    }

    @Test
    fun key_editedImage_differs() {
        assertNotEquals(
            keyer.key(thumbnail, options()),
            keyer.key(thumbnail.copy(dateModifiedSeconds = 200), options()),
        )
    }

    @Test
    fun key_otherSize_differs() {
        assertNotEquals(keyer.key(thumbnail, options(side = 256)), keyer.key(thumbnail, options(side = 512)))
    }
}
