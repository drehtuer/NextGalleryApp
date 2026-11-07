package net.drehtuer.nextgallery.core.data.folders

import net.drehtuer.nextgallery.core.model.AddedFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreeDocumentIdsTest {
    @Test
    fun parse_primaryStorage_isThePrimaryVolume() {
        assertEquals(AddedFolder("external_primary", "DCIM/Camera"), TreeDocumentIds.parse("primary:DCIM/Camera"))
    }

    @Test
    fun parse_sdCard_isItsVolumeInLowerCase() {
        assertEquals(AddedFolder("1234-abcd", "DCIM"), TreeDocumentIds.parse("1234-ABCD:DCIM"))
    }

    @Test
    fun parse_colonInFolderName_staysInThePath() {
        assertEquals(AddedFolder("external_primary", "a:b"), TreeDocumentIds.parse("primary:a:b"))
    }

    @Test
    fun parse_trailingSlash_isRemoved() {
        assertEquals(AddedFolder("external_primary", "DCIM"), TreeDocumentIds.parse("primary:DCIM/"))
    }

    @Test
    fun parse_storageRoot_isNull() {
        assertNull(TreeDocumentIds.parse("primary:"))
    }

    @Test
    fun parse_sdCardRoot_isNull() {
        assertNull(TreeDocumentIds.parse("1234-ABCD:"))
    }

    @Test
    fun parse_noSeparator_isNull() {
        assertNull(TreeDocumentIds.parse("downloads"))
    }

    @Test
    fun parse_empty_isNull() {
        assertNull(TreeDocumentIds.parse(""))
    }

    @Test
    fun parse_idOfAnotherRoot_isNull() {
        assertNull(TreeDocumentIds.parse("raw:/storage/emulated/0/Download/Photos"))
    }
}
