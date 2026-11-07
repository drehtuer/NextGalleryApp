package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddedFolderTest {
    @Test
    fun name_isTheLastPartOfThePath() {
        assertEquals("Camera", AddedFolder(AddedFolder.PRIMARY_VOLUME, "DCIM/Camera").name)
    }

    @Test
    fun isOnPrimaryVolume_primary_isTrue() {
        assertTrue(AddedFolder(AddedFolder.PRIMARY_VOLUME, "DCIM").isOnPrimaryVolume)
    }

    @Test
    fun isOnPrimaryVolume_sdCard_isFalse() {
        assertFalse(AddedFolder("1234-abcd", "DCIM").isOnPrimaryVolume)
    }
}
