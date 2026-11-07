package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.model.CertificateInfo
import java.security.MessageDigest
import java.security.cert.X509Certificate

object CertificateInfoMapper {
    private const val BYTE_MASK = 0xFF
    private const val HEX_DIGITS = 2
    private const val HEX_RADIX = 16

    fun map(host: String, certificate: X509Certificate, pinnedFingerprint: String? = null) = CertificateInfo(
        host = host,
        subject = certificate.subjectX500Principal.name,
        issuer = certificate.issuerX500Principal.name,
        notBeforeMillis = certificate.notBefore.time,
        notAfterMillis = certificate.notAfter.time,
        sha256Fingerprint = fingerprint(certificate),
        pinnedFingerprint = pinnedFingerprint,
    )

    /** SHA-256 of the whole certificate as "AB:CD:…", the form the dialog shows. */
    fun fingerprint(certificate: X509Certificate): String = MessageDigest.getInstance("SHA-256")
        .digest(certificate.encoded)
        .joinToString(":") { byte ->
            (byte.toInt() and BYTE_MASK).toString(radix = HEX_RADIX).uppercase().padStart(HEX_DIGITS, '0')
        }
}
