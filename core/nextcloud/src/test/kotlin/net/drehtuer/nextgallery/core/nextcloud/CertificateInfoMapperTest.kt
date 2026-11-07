package net.drehtuer.nextgallery.core.nextcloud

import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.MessageDigest

class CertificateInfoMapperTest {
    private val ca = HeldCertificate.Builder().certificateAuthority(0).commonName("Test CA").build()
    private val held = HeldCertificate.Builder()
        .commonName("cloud.example.com")
        .validityInterval(1_000, 2_000_000)
        .signedBy(ca)
        .build()

    @Test
    fun map_readsTheFields() {
        val info = CertificateInfoMapper.map("cloud.example.com", held.certificate)

        assertEquals("cloud.example.com", info.host)
        assertEquals(held.certificate.subjectX500Principal.name, info.subject)
        assertEquals(ca.certificate.subjectX500Principal.name, info.issuer)
        assertEquals(1_000L, info.notBeforeMillis)
        assertEquals(2_000_000L, info.notAfterMillis)
        assertNull(info.pinnedFingerprint)
    }

    @Test
    fun fingerprint_isTheSha256OfTheCertificateInHexPairs() {
        val expected = MessageDigest.getInstance("SHA-256").digest(held.certificate.encoded)
            .joinToString(":") { "%02X".format(it) }

        assertEquals(expected, CertificateInfoMapper.fingerprint(held.certificate))
    }
}
