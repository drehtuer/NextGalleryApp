package net.drehtuer.nextgallery.core.data.settings

import net.drehtuer.nextgallery.core.model.AddedFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddedFolderCodecTest {
    @Test
    fun encodeThenDecode_returnsTheFolder() {
        val folder = AddedFolder("external_primary", "DCIM/Camera")

        assertEquals(folder, AddedFolderCodec.decode(AddedFolderCodec.encode(folder)))
    }

    @Test
    fun decode_colonInPath_staysInThePath() {
        assertEquals(AddedFolder("1234-abcd", "a:b"), AddedFolderCodec.decode("1234-abcd:a:b"))
    }

    @Test
    fun decode_noSeparator_isNull() {
        assertNull(AddedFolderCodec.decode("DCIM"))
    }

    @Test
    fun decode_noPath_isNull() {
        assertNull(AddedFolderCodec.decode("external_primary:"))
    }

    @Test
    fun decode_noVolume_isNull() {
        assertNull(AddedFolderCodec.decode(":DCIM"))
    }
}
