package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.nextcloud.TestTls.serve
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/** Real TLS handshakes against a local server. A failure must mean that no request reached the server. */
class PinningTrustManagerTest {
    private val server = MockWebServer()
    private val pins = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        server.enqueue(MockResponse().setBody("ok"))
    }

    @After
    fun tearDown() = server.shutdown()

    private fun module() = TestTls.module(pins = { host -> pins[host] })

    private fun fetch(module: NextcloudHttpModule = module()): String {
        val request = Request.Builder().url(server.url("/").newBuilder().host("localhost").build()).build()
        return module.base.newCall(request).execute().use { it.body.string() }
    }

    private fun fetchError(): AppError = NextcloudErrorMapper.map(assertThrows(IOException::class.java) { fetch() })

    private fun fingerprint(certificate: X509Certificate) = CertificateInfoMapper.fingerprint(certificate)

    @Test
    fun caSignedCertificate_isAccepted() {
        server.serve(TestTls.signedByTrustedCa())

        assertEquals("ok", fetch())
    }

    @Test
    fun unknownSelfSignedCertificate_isRejectedAsUntrusted() {
        server.serve(TestTls.selfSigned())

        val error = fetchError() as AppError.UntrustedCertificate

        assertEquals("localhost", error.info.host)
        assertFalse(error.info.changedFromPinned)
    }

    @Test
    fun unknownSelfSignedCertificate_sendsNothingToTheServer() {
        server.serve(TestTls.selfSigned())

        fetchError()

        assertEquals(0, server.requestCount)
    }

    @Test
    fun pinnedSelfSignedCertificate_isAccepted() {
        val certificate = TestTls.selfSigned()
        server.serve(certificate)
        pins["localhost"] = fingerprint(certificate.certificate)

        assertEquals("ok", fetch())
    }

    @Test
    fun changedCertificate_isRejectedWithChangedFromPinned() {
        server.serve(TestTls.selfSigned())
        pins["localhost"] = "AA:BB"

        val error = fetchError() as AppError.UntrustedCertificate

        assertTrue(error.info.changedFromPinned)
        assertEquals("AA:BB", error.info.pinnedFingerprint)
    }

    @Test
    fun changedCertificate_sendsNothingToTheServer() {
        server.serve(TestTls.selfSigned())
        pins["localhost"] = "AA:BB"

        fetchError()

        assertEquals(0, server.requestCount)
    }

    @Test
    fun pinForAnotherHost_doesNotApply() {
        val certificate = TestTls.selfSigned()
        server.serve(certificate)
        pins["cloud.example.com"] = fingerprint(certificate.certificate)

        assertTrue(fetchError() is AppError.UntrustedCertificate)
    }

    @Test
    fun expiredCaSignedCertificate_isRejectedAsUntrusted() {
        server.serve(TestTls.expiredSignedByTrustedCa())

        assertTrue(fetchError() is AppError.UntrustedCertificate)
    }

    @Test
    fun caSignedCertificateWithAnotherHostname_isRejectedAsUntrusted() {
        server.serve(TestTls.signedByTrustedCa(host = "other.example.com"))

        assertTrue(fetchError() is AppError.UntrustedCertificate)
    }

    @Test
    fun caSignedCertificateWithAnotherHostname_sendsNothingToTheServer() {
        server.serve(TestTls.signedByTrustedCa(host = "other.example.com"))

        fetchError()

        assertEquals(0, server.requestCount)
    }

    @Test
    fun selfSignedCertificateWithAnotherHostname_isRejectedAsUntrusted() {
        server.serve(TestTls.selfSigned(host = "other.example.com"))

        assertTrue(fetchError() is AppError.UntrustedCertificate)
    }

    @Test
    fun pinnedCertificateWithHostnameMismatch_isAccepted() {
        val certificate = TestTls.selfSigned(host = "other.example.com")
        server.serve(certificate)
        pins["localhost"] = fingerprint(certificate.certificate)

        assertEquals("ok", fetch())
    }

    @Test
    fun pinnedCaSignedCertificateWithHostnameMismatch_isAccepted() {
        val certificate = TestTls.signedByTrustedCa(host = "other.example.com")
        server.serve(certificate)
        pins["localhost"] = fingerprint(certificate.certificate)

        assertEquals("ok", fetch())
    }

    @Test
    fun hostnameMismatchWithAnotherPin_isRejectedWithChangedFromPinned() {
        server.serve(TestTls.signedByTrustedCa(host = "other.example.com"))
        pins["localhost"] = "AA:BB"

        val error = fetchError() as AppError.UntrustedCertificate

        assertTrue(error.info.changedFromPinned)
    }

    @Test
    fun removedPin_isRejectedAfterConnectionsAreEvicted() {
        val certificate = TestTls.selfSigned()
        server.serve(certificate)
        server.enqueue(MockResponse().setBody("ok"))
        pins["localhost"] = fingerprint(certificate.certificate)
        val module = module()
        fetch(module)

        pins.clear()
        module.evictConnections()

        val error = NextcloudErrorMapper.map(assertThrows(IOException::class.java) { fetch(module) })
        assertTrue(error is AppError.UntrustedCertificate)
    }

    @Test
    fun checkServerTrusted_withoutHost_doesNotUsePins() {
        val certificate = TestTls.selfSigned().certificate
        val manager = PinningTrustManager(TestTls.platformTrust) { fingerprint(certificate) }

        assertThrows(CertificateException::class.java) {
            manager.checkServerTrusted(arrayOf(certificate), "ECDHE_ECDSA")
        }
    }

    @Test
    fun checkServerTrusted_emptyChainWithHost_isRejected() {
        val refusing = object : X509TrustManager by TestTls.platformTrust {}
        val manager = PinningTrustManager(refusing) { "AA:BB" }

        assertThrows(Exception::class.java) { manager.checkServerTrusted(emptyArray(), "ECDHE_ECDSA") }
    }

    @Test
    fun checkClientTrusted_isAlwaysRefused() {
        val manager = PinningTrustManager(TestTls.platformTrust) { null }

        assertThrows(CertificateException::class.java) {
            manager.checkClientTrusted(arrayOf(TestTls.trustedCa.certificate), "RSA")
        }
    }

    @Test
    fun acceptedIssuers_comeFromThePlatform() {
        val manager = PinningTrustManager(TestTls.platformTrust) { null }

        assertEquals(TestTls.platformTrust.acceptedIssuers.toList(), manager.acceptedIssuers.toList())
    }
}
