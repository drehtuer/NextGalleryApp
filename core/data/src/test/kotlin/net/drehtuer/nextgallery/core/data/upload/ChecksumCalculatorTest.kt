package net.drehtuer.nextgallery.core.data.upload

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class ChecksumCalculatorTest {
    private fun stream(text: String) = text.byteInputStream()

    @Test
    fun sha1_knownValue() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", ChecksumCalculator.sha1(stream("abc")))
    }

    @Test
    fun md5_knownValue() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", ChecksumCalculator.md5(stream("abc")))
    }

    @Test
    fun sha1_emptyFile_knownValue() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", ChecksumCalculator.sha1(stream("")))
    }

    @Test
    fun digest_fileLargerThanTheBuffer_coversAllOfIt() {
        val bytes = ByteArray(200_000) { it.toByte() }
        val expected = MessageDigest.getInstance("SHA-1").digest(bytes).toHexString()

        assertEquals(expected, ChecksumCalculator.digest(ChecksumAlgorithm.SHA1, bytes.inputStream()))
    }
}
