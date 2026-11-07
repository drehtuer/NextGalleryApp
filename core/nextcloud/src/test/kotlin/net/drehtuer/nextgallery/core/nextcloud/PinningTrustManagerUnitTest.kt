package net.drehtuer.nextgallery.core.nextcloud

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Socket
import java.security.cert.CertificateException
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/** The paths a handshake against the local test server doesn't reach. `PinningTrustManagerTest` has the real ones. */
class PinningTrustManagerUnitTest {
    private val certificate = TestTls.selfSigned().certificate
    private val chain = arrayOf(certificate)
    private val fingerprint = CertificateInfoMapper.fingerprint(certificate)
    private val pinned = CertificatePinSource { host -> fingerprint.takeIf { host == "cloud.example.com" } }

    /** A platform trust manager without the host-aware methods, which rejects everything. */
    private val plainPlatform = mockk<X509TrustManager> {
        every { checkServerTrusted(any(), any()) } throws CertificateException("Not trusted")
    }

    private val handshake = mockk<SSLSession> { every { peerHost } returns "cloud.example.com" }
    private val socket = mockk<SSLSocket>(relaxed = true) { every { handshakeSession } returns handshake }
    private val engine = mockk<SSLEngine>(relaxed = true) { every { handshakeSession } returns handshake }

    private fun assertUntrusted(block: () -> Unit): UntrustedCertificateException =
        assertThrows(UntrustedCertificateException::class.java) { block() }

    @Test
    fun socket_plainPlatform_pinnedIsAccepted() {
        PinningTrustManager(plainPlatform, pinned).checkServerTrusted(chain, "RSA", socket)
    }

    @Test
    fun socket_plainPlatform_unknownIsUntrusted() {
        val manager = PinningTrustManager(plainPlatform) { null }

        val error = assertUntrusted { manager.checkServerTrusted(chain, "RSA", socket) }

        assertFalse(error.info.changedFromPinned)
    }

    @Test
    fun socket_upperCaseHost_findsThePin() {
        every { handshake.peerHost } returns "Cloud.Example.COM"

        PinningTrustManager(plainPlatform, pinned).checkServerTrusted(chain, "RSA", socket)
    }

    @Test
    fun socket_noHandshakeSession_doesNotUsePins() {
        every { socket.handshakeSession } returns null
        val manager = PinningTrustManager(plainPlatform, pinned)

        val error = assertThrows(CertificateException::class.java) { manager.checkServerTrusted(chain, "RSA", socket) }

        assertFalse(error is UntrustedCertificateException)
    }

    @Test
    fun socket_notAnSslSocket_doesNotUsePins() {
        val manager = PinningTrustManager(plainPlatform, pinned)

        val error = assertThrows(CertificateException::class.java) {
            manager.checkServerTrusted(chain, "RSA", Socket())
        }

        assertFalse(error is UntrustedCertificateException)
    }

    @Test
    fun socket_emptyChain_isRejectedEvenWithAPin() {
        val manager = PinningTrustManager(plainPlatform, pinned)

        assertThrows(CertificateException::class.java) { manager.checkServerTrusted(emptyArray(), "RSA", socket) }
    }

    @Test
    fun engine_pinnedIsAccepted() {
        PinningTrustManager(plainPlatform, pinned).checkServerTrusted(chain, "RSA", engine)
    }

    @Test
    fun engine_unknownIsUntrusted() {
        val manager = PinningTrustManager(plainPlatform) { null }

        assertEquals(
            "cloud.example.com",
            assertUntrusted {
                manager.checkServerTrusted(chain, "RSA", engine)
            }.info.host,
        )
    }

    @Test
    fun engine_extendedPlatform_stillRejectsUnknown() {
        val manager = PinningTrustManager(TestTls.platformTrust) { null }

        assertUntrusted { manager.checkServerTrusted(chain, "RSA", engine) }
    }

    @Test
    fun engine_noHost_doesNotUsePins() {
        val noHost = mockk<SSLEngine>(relaxed = true) {
            every { handshakeSession } returns null
            every { peerHost } returns null
        }
        val manager = PinningTrustManager(plainPlatform, pinned)

        val error = assertThrows(CertificateException::class.java) { manager.checkServerTrusted(chain, "RSA", noHost) }

        assertFalse(error is UntrustedCertificateException)
    }

    @Test
    fun checkClientTrusted_withSocket_isRefused() {
        val manager = PinningTrustManager(plainPlatform, pinned)

        assertThrows(CertificateException::class.java) { manager.checkClientTrusted(chain, "RSA", socket) }
    }

    @Test
    fun checkClientTrusted_withEngine_isRefused() {
        val manager = PinningTrustManager(plainPlatform, pinned)

        assertThrows(CertificateException::class.java) { manager.checkClientTrusted(chain, "RSA", engine) }
    }

    @Test
    fun hostnameVerifier_noCertificate_isRejected() {
        val verifier = PinningHostnameVerifier({ _, _ -> false }, pinned)
        val empty = mockk<SSLSession> { every { peerCertificates } returns emptyArray() }

        assertThrows(javax.net.ssl.SSLPeerUnverifiedException::class.java) {
            verifier.verify("cloud.example.com", empty)
        }
    }

    @Test
    fun hostnameVerifier_platformAccepts_doesNotLookAtPins() {
        val verifier = PinningHostnameVerifier({ _, _ -> true }) { error("Pins must not be needed") }

        assertTrue(verifier.verify("cloud.example.com", mockk<SSLSession>()))
    }
}
