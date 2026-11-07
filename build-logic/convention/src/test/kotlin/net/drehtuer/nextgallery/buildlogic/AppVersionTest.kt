package net.drehtuer.nextgallery.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    @Test
    fun fromTag_dropsTheLeadingV() {
        assertEquals("1.2.3", AppVersion.fromTag("v1.2.3").name)
    }

    @Test
    fun fromTag_combinesTheNumbersIntoTheCode() {
        assertEquals(1_002_003, AppVersion.fromTag("v1.2.3").code)
    }

    @Test
    fun fromTag_laterPatchHasAHigherCode() {
        assertTrue(AppVersion.fromTag("v1.2.4").code > AppVersion.fromTag("v1.2.3").code)
    }

    @Test
    fun fromTag_laterMinorHasAHigherCodeThanAnyPatch() {
        assertTrue(AppVersion.fromTag("v1.3.0").code > AppVersion.fromTag("v1.2.999").code)
    }

    @Test
    fun fromTag_laterMajorHasAHigherCodeThanAnyMinor() {
        assertTrue(AppVersion.fromTag("v2.0.0").code > AppVersion.fromTag("v1.999.999").code)
    }

    @Test
    fun fromTag_withoutV_isRefused() {
        assertThrows(IllegalArgumentException::class.java) { AppVersion.fromTag("1.2.3") }
    }

    @Test
    fun fromTag_preRelease_isRefused() {
        assertThrows(IllegalArgumentException::class.java) { AppVersion.fromTag("v1.2.3-rc1") }
    }

    @Test
    fun fromTag_zeroVersion_isRefused() {
        assertThrows(IllegalArgumentException::class.java) { AppVersion.fromTag("v0.0.0") }
    }

    @Test
    fun fromTag_codeAboveAndroidsLimit_isRefused() {
        assertThrows(IllegalArgumentException::class.java) { AppVersion.fromTag("v2101.0.0") }
    }

    @Test
    fun fallbackTag_isValid() {
        assertEquals("0.1.0", AppVersion.fromTag(AppVersion.FALLBACK_TAG).name)
    }
}
