package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CertificateInfoTest {
    private val info = CertificateInfo("cloud.example.com", "CN=a", "CN=b", 1, 2, "AA:BB")

    @Test
    fun changedFromPinned_withoutEarlierCertificate_isFalse() {
        assertFalse(info.changedFromPinned)
    }

    @Test
    fun changedFromPinned_withEarlierCertificate_isTrue() {
        assertTrue(info.copy(pinnedFingerprint = "CC:DD").changedFromPinned)
    }
}
