package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedMimeTypesTest {
    @Test
    fun isSupported_eachSupportedType_returnsTrue() {
        listOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif", "image/gif").forEach {
            assertTrue(it, SupportedMimeTypes.isSupported(it))
        }
    }

    @Test
    fun isSupported_video_returnsFalse() {
        assertFalse(SupportedMimeTypes.isSupported("video/mp4"))
    }

    @Test
    fun isSupported_upperCase_returnsTrue() {
        assertTrue(SupportedMimeTypes.isSupported("IMAGE/JPEG"))
    }

    @Test
    fun isEditable_jpegAndPng_returnTrue() {
        assertTrue(SupportedMimeTypes.isEditable("image/jpeg") && SupportedMimeTypes.isEditable("image/png"))
    }

    @Test
    fun isEditable_otherSupportedTypes_returnFalse() {
        listOf("image/webp", "image/heic", "image/heif", "image/gif").forEach {
            assertFalse(it, SupportedMimeTypes.isEditable(it))
        }
    }
}
