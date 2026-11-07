package net.drehtuer.nextgallery.core.data.settings

import net.drehtuer.nextgallery.core.model.FolderRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderRuleCodecTest {
    @Test
    fun encode_thenDecode_returnsSameRule() {
        val rule = FolderRule("DCIM/Camera", FolderRule.Type.INCLUDE)

        assertEquals(rule, FolderRuleCodec.decode(FolderRuleCodec.encode(rule)))
    }

    @Test
    fun decode_colonInPath_keepsWholePath() {
        assertEquals(FolderRule("a:b", FolderRule.Type.EXCLUDE), FolderRuleCodec.decode("EXCLUDE:a:b"))
    }

    @Test
    fun decode_unknownType_returnsNull() {
        assertNull(FolderRuleCodec.decode("MAYBE:DCIM"))
    }
}
