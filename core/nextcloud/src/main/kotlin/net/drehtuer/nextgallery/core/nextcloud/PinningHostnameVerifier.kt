package net.drehtuer.nextgallery.core.nextcloud

import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession

/**
 * The normal hostname check, except that the certificate the user pinned for this host may have another name (D32).
 * Any other mismatch fails with an [UntrustedCertificateException] as cause, so the user sees the warning dialog.
 */
class PinningHostnameVerifier(private val platform: HostnameVerifier, private val pins: CertificatePinSource) :
    HostnameVerifier {
    override fun verify(hostname: String, session: SSLSession): Boolean {
        if (platform.verify(hostname, session)) return true
        val leaf = session.peerCertificates.firstOrNull() as? X509Certificate
            ?: throw SSLPeerUnverifiedException("No certificate for $hostname")
        val pinned = pins.pinnedFingerprint(hostname.lowercase())
        if (pinned == CertificateInfoMapper.fingerprint(leaf)) return true
        throw SSLPeerUnverifiedException("Hostname mismatch for $hostname").apply {
            initCause(UntrustedCertificateException(CertificateInfoMapper.map(hostname, leaf, pinned)))
        }
    }
}
