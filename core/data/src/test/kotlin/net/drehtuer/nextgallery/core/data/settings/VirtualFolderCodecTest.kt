package net.drehtuer.nextgallery.core.data.settings

import net.drehtuer.nextgallery.core.model.VirtualFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VirtualFolderCodecTest {
    private fun roundTrip(folder: VirtualFolder) = VirtualFolderCodec.decode(VirtualFolderCodec.encode(folder))

    @Test
    fun encodeThenDecode_returnsTheFolder() {
        val folder = VirtualFolder("v1", "Everything", listOf(7, -8))

        assertEquals(folder, roundTrip(folder))
    }

    @Test
    fun encodeThenDecode_separatorsInName_stayInTheName() {
        val folder = VirtualFolder("v1", "a:b,c", listOf(7))

        assertEquals(folder, roundTrip(folder))
    }

    @Test
    fun encodeThenDecode_noMembers_staysEmpty() {
        val folder = VirtualFolder("v1", "Everything", emptyList())

        assertEquals(folder, roundTrip(folder))
    }

    @Test
    fun decode_tooFewParts_isNull() {
        assertNull(VirtualFolderCodec.decode("v1:7"))
    }

    @Test
    fun decode_noId_isNull() {
        assertNull(VirtualFolderCodec.decode(":7:Everything"))
    }

    @Test
    fun decode_noName_isNull() {
        assertNull(VirtualFolderCodec.decode("v1:7:"))
    }

    @Test
    fun decode_memberIsNotANumber_isNull() {
        assertNull(VirtualFolderCodec.decode("v1:7,x:Everything"))
    }
}
