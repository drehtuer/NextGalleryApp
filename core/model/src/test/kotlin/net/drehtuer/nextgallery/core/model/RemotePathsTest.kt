package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePathsTest {
    @Test
    fun normalize_emptyPath_returnsRoot() {
        assertEquals("/", RemotePaths.normalize(""))
    }

    @Test
    fun normalize_addsLeadingAndRemovesTrailingSlash() {
        assertEquals("/Photos/2024", RemotePaths.normalize("Photos//2024/"))
    }

    @Test
    fun parent_ofTopLevelItem_isRoot() {
        assertEquals("/", RemotePaths.parent("/Photos"))
    }

    @Test
    fun parent_ofNestedItem_isItsFolder() {
        assertEquals("/Photos/2024", RemotePaths.parent("/Photos/2024/IMG_1.jpg"))
    }

    @Test
    fun name_returnsLastSegment() {
        assertEquals("IMG_1.jpg", RemotePaths.name("/Photos/IMG_1.jpg"))
    }

    @Test
    fun name_ofRoot_isEmpty() {
        assertEquals("", RemotePaths.name("/"))
    }

    @Test
    fun join_atRoot_hasOneSlash() {
        assertEquals("/Photos", RemotePaths.join("/", "Photos"))
    }

    @Test
    fun ancestors_listFromRootToPath() {
        assertEquals(listOf("/", "/Photos", "/Photos/2024"), RemotePaths.ancestors("/Photos/2024"))
    }

    @Test
    fun isValidName_plainName_isTrue() {
        assertTrue(RemotePaths.isValidName("Holiday 2024"))
    }

    @Test
    fun isValidName_blank_isFalse() {
        assertFalse(RemotePaths.isValidName("  "))
    }

    @Test
    fun isValidName_dotDot_isFalse() {
        assertFalse(RemotePaths.isValidName(".."))
    }

    @Test
    fun isValidName_withSlash_isFalse() {
        assertFalse(RemotePaths.isValidName("a/b"))
    }

    @Test
    fun isValidName_withBackslash_isFalse() {
        assertFalse(RemotePaths.isValidName("a\\b"))
    }

    @Test
    fun isValidName_withControlCharacter_isFalse() {
        assertFalse(RemotePaths.isValidName("a\u0000b"))
    }

    @Test
    fun isValidName_withLineBreak_isFalse() {
        assertFalse(RemotePaths.isValidName("a\nb"))
    }
}
