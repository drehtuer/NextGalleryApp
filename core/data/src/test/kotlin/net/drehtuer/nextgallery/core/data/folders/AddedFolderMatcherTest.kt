package net.drehtuer.nextgallery.core.data.folders

import net.drehtuer.nextgallery.core.model.AddedFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddedFolderMatcherTest {
    private val volume = "external_primary"
    private val matcher = AddedFolderMatcher(listOf(AddedFolder(volume, "DCIM/Camera")))

    @Test
    fun isIndexed_theAddedFolder_isTrue() {
        assertTrue(matcher.isIndexed(volume, "DCIM/Camera/"))
    }

    @Test
    fun isIndexed_subfolder_isTrue() {
        assertTrue(matcher.isIndexed(volume, "DCIM/Camera/2024/"))
    }

    @Test
    fun isIndexed_otherCase_isTrue() {
        assertTrue(matcher.isIndexed(volume, "dcim/camera/"))
    }

    @Test
    fun isIndexed_parentFolder_isFalse() {
        assertFalse(matcher.isIndexed(volume, "DCIM/"))
    }

    @Test
    fun isIndexed_folderWithTheSamePrefix_isFalse() {
        assertFalse(matcher.isIndexed(volume, "DCIM/Camera2/"))
    }

    @Test
    fun isIndexed_otherVolume_isFalse() {
        assertFalse(matcher.isIndexed("1234-abcd", "DCIM/Camera/"))
    }

    @Test
    fun isIndexed_noAddedFolder_isFalse() {
        assertFalse(AddedFolderMatcher(emptyList()).isIndexed(volume, "DCIM/Camera/"))
    }

    @Test
    fun isIndexed_hiddenFolderBelow_isFalse() {
        assertFalse(matcher.isIndexed(volume, "DCIM/Camera/.thumbnails/"))
    }

    @Test
    fun isIndexed_folderBelowAHiddenFolder_isFalse() {
        assertFalse(matcher.isIndexed(volume, "DCIM/Camera/.hidden/deep/"))
    }

    @Test
    fun isIndexed_hiddenFolderAddedOnPurpose_isTrue() {
        val matcher = AddedFolderMatcher(listOf(AddedFolder(volume, "WhatsApp/Media/.Statuses")))

        assertTrue(matcher.isIndexed(volume, "WhatsApp/Media/.Statuses/"))
    }

    @Test
    fun isIndexed_hiddenFolderAbove_isTrue() {
        val matcher = AddedFolderMatcher(listOf(AddedFolder(volume, ".hidden/Photos")))

        assertTrue(matcher.isIndexed(volume, ".hidden/Photos/2024/"))
    }

    @Test
    fun pathsOn_returnsOnlyTheFoldersOfThatVolume() {
        val matcher = AddedFolderMatcher(listOf(AddedFolder(volume, "DCIM/"), AddedFolder("1234-abcd", "Pictures")))

        assertEquals(listOf("DCIM"), matcher.pathsOn(volume))
    }

    @Test
    fun pathsOn_volumeWithoutFolder_isEmpty() {
        assertEquals(emptyList<String>(), matcher.pathsOn("1234-abcd"))
    }
}
