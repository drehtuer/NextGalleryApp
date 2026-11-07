package net.drehtuer.nextgallery.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ReleaseSigningTest {
    private val complete = """
        storeFile=release.jks
        storePassword=store-secret
        keyAlias=nextgallery
        keyPassword=key-secret
    """.trimIndent()

    @Test
    fun parse_noFile_meansUnsigned() {
        assertNull(ReleaseSigning.parse(null))
    }

    @Test
    fun parse_completeFile_readsAllValues() {
        assertEquals(
            ReleaseSigning("release.jks", "store-secret", "nextgallery", "key-secret"),
            ReleaseSigning.parse(complete),
        )
    }

    @Test
    fun parse_valueMissing_isRefused() {
        val withoutAlias = complete.lines().filterNot { it.startsWith("keyAlias") }.joinToString("\n")

        assertThrows(IllegalArgumentException::class.java) { ReleaseSigning.parse(withoutAlias) }
    }

    @Test
    fun parse_valueBlank_isRefused() {
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseSigning.parse(complete.replace("key-secret", " "))
        }
    }

    @Test
    fun toString_hidesThePasswords() {
        val text = ReleaseSigning.parse(complete).toString()

        assertFalse(text.contains("secret"))
    }
}
