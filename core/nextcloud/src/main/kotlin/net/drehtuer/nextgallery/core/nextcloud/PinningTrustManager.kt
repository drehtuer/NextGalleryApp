package net.drehtuer.nextgallery.core.nextcloud

import java.net.Socket
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

/** The certificates the user accepted, per host (D32). Called during the TLS handshake. */
fun interface CertificatePinSource {
    fun pinnedFingerprint(host: String): String?
}

/**
 * Platform trust first (system and user CAs, through the network security config), then the certificate the user
 * pinned for exactly this host (D32). Everything else fails with [UntrustedCertificateException]: nothing is accepted
 * without an explicit pin, and when the host cannot be determined, pins do not apply at all.
 */
class PinningTrustManager(private val platform: X509TrustManager, private val pins: CertificatePinSource) :
    X509ExtendedTrustManager() {
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        val host = (socket as? SSLSocket)?.handshakeSession?.peerHost
        checkWithPins(chain, host) {
            if (platform is X509ExtendedTrustManager) {
                platform.checkServerTrusted(chain, authType, socket)
            } else {
                platform.checkServerTrusted(chain, authType)
            }
        }
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
        checkWithPins(chain, engine.handshakeSession?.peerHost ?: engine.peerHost) {
            if (platform is X509ExtendedTrustManager) {
                platform.checkServerTrusted(chain, authType, engine)
            } else {
                platform.checkServerTrusted(chain, authType)
            }
        }
    }

    /** Without a host there is nothing a pin could be looked up for, so only the platform can accept. */
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        platform.checkServerTrusted(chain, authType)
    }

    private inline fun checkWithPins(chain: Array<X509Certificate>, host: String?, platformCheck: () -> Unit) {
        try {
            platformCheck()
        } catch (platformFailure: CertificateException) {
            val leaf = chain.firstOrNull()
            if (host == null || leaf == null) throw platformFailure
            val pinned = pins.pinnedFingerprint(host.lowercase())
            if (pinned != CertificateInfoMapper.fingerprint(leaf)) {
                throw UntrustedCertificateException(CertificateInfoMapper.map(host, leaf, pinned)).apply {
                    initCause(platformFailure)
                }
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = platform.acceptedIssuers

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String): Unit = throw clientRefused()

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket): Unit =
        throw clientRefused()

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine): Unit =
        throw clientRefused()

    private fun clientRefused() = CertificateException("This app is never a TLS server")
}
