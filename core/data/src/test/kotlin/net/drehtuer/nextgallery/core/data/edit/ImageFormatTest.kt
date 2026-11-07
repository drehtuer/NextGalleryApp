package net.drehtuer.nextgallery.core.data.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageFormatTest {
    @Test
    fun fromMimeType_jpeg() {
        assertEquals(ImageFormat.JPEG, ImageFormat.fromMimeType("image/jpeg"))
    }

    @Test
    fun fromMimeType_png_ignoresCase() {
        assertEquals(ImageFormat.PNG, ImageFormat.fromMimeType("IMAGE/PNG"))
    }

    @Test
    fun fromMimeType_otherFormat_isNull() {
        assertNull(ImageFormat.fromMimeType("image/heic"))
    }
}
