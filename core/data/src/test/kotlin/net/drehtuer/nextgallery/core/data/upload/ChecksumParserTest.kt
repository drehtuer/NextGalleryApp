package net.drehtuer.nextgallery.core.data.upload

import org.junit.Assert.assertEquals
import org.junit.Test

class ChecksumParserTest {
    @Test
    fun parse_null_isEmpty() {
        assertEquals(emptyList<RemoteChecksum>(), ChecksumParser.parse(null))
    }

    @Test
    fun parse_blank_isEmpty() {
        assertEquals(emptyList<RemoteChecksum>(), ChecksumParser.parse("  "))
    }

    @Test
    fun parse_bothAlgorithms_bestFirst() {
        val expected = listOf(RemoteChecksum(ChecksumAlgorithm.SHA1, "aa"), RemoteChecksum(ChecksumAlgorithm.MD5, "bb"))

        assertEquals(expected, ChecksumParser.parse("MD5:bb SHA1:aa"))
    }

    @Test
    fun parse_unsupportedAlgorithm_isLeftOut() {
        assertEquals(emptyList<RemoteChecksum>(), ChecksumParser.parse("ADLER32:cc"))
    }

    @Test
    fun parse_lowerCaseName_isRecognised() {
        assertEquals(listOf(RemoteChecksum(ChecksumAlgorithm.MD5, "bb")), ChecksumParser.parse("md5:bb"))
    }

    @Test
    fun parse_noValue_isLeftOut() {
        assertEquals(emptyList<RemoteChecksum>(), ChecksumParser.parse("SHA1:"))
    }

    @Test
    fun parse_noSeparator_isLeftOut() {
        assertEquals(emptyList<RemoteChecksum>(), ChecksumParser.parse("SHA1"))
    }
}
