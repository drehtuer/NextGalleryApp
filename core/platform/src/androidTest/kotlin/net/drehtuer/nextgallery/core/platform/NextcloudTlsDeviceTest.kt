package net.drehtuer.nextgallery.core.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.nextcloud.CertificateInfoMapper
import net.drehtuer.nextgallery.core.nextcloud.CertificatePinSource
import net.drehtuer.nextgallery.core.nextcloud.NextcloudErrorMapper
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpConfig
import net.drehtuer.nextgallery.core.nextcloud.NextcloudHttpModule
import okhttp3.Request
import okhttp3.TlsVersion
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Real handshakes on Android's own TLS stack and system trust store, which the JVM tests cannot reach: an unknown
 * certificate must reach the warning dialog as [AppError.UntrustedCertificate] (D32), not as a plain network error.
 * The server runs on this device, so nothing leaves it.
 */
@RunWith(AndroidJUnit4::class)
class NextcloudTlsDeviceTest {
    private val server = MockWebServer()

    @After
    fun stopServer() = server.shutdown()

    private fun certificate(host: String = "localhost") = HeldCertificate.Builder()
        .commonName("NextGallery test server")
        .addSubjectAlternativeName(host)
        .build()

    private fun serve(certificate: HeldCertificate) {
        val certificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        server.useHttps(certificates.sslSocketFactory(), false)
        server.enqueue(MockResponse().setBody("ok"))
        server.start()
    }

    private fun request(pins: CertificatePinSource): Result<Int> {
        val module = NextcloudHttpModule(
            config = NextcloudHttpConfig("NextGalleryApp/test", allowCleartext = false),
            credentials = { null },
            pins = pins,
        )
        val url = server.url("/").newBuilder().host("localhost").build()
        return runCatching {
            module.base.newCall(Request.Builder().url(url).build()).execute().use { it.code }
        }
    }

    private fun Result<Int>.error(): AppError {
        val failure = exceptionOrNull()
        assertTrue("Expected a failure, got $this", failure is IOException)
        return NextcloudErrorMapper.map(checkNotNull(failure))
    }

    /** Self-hosted servers may offer nothing newer. */
    @Test
    fun serverWithOnlyTls12_connects() {
        val certificate = certificate()
        val certificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        server.useHttps(Tls12Only(certificates.sslSocketFactory()), false)
        server.enqueue(MockResponse().setBody("ok"))
        server.start()
        val fingerprint = CertificateInfoMapper.fingerprint(certificate.certificate)
        val module = NextcloudHttpModule(
            config = NextcloudHttpConfig("NextGalleryApp/test", allowCleartext = false),
            credentials = { null },
            pins = { fingerprint },
        )
        val url = server.url("/").newBuilder().host("localhost").build()

        val version = module.base.newCall(Request.Builder().url(url).build()).execute().use { it.handshake?.tlsVersion }

        assertEquals(TlsVersion.TLS_1_2, version)
    }

    /** Offers a server socket that only speaks TLS 1.2. */
    private class Tls12Only(private val delegate: SSLSocketFactory) : SSLSocketFactory() {
        private fun Socket.limited() = apply { (this as SSLSocket).enabledProtocols = arrayOf("TLSv1.2") }

        override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

        override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

        override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket =
            delegate.createSocket(socket, host, port, autoClose).limited()

        override fun createSocket(host: String, port: Int): Socket = delegate.createSocket(host, port).limited()

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
            delegate.createSocket(host, port, localHost, localPort).limited()

        override fun createSocket(host: InetAddress, port: Int): Socket = delegate.createSocket(host, port).limited()

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
            delegate.createSocket(address, port, localAddress, localPort).limited()
    }

    @Test
    fun unknownCertificate_reportsUntrustedCertificate() {
        serve(certificate())

        val error = request { null }.error()

        assertFalse((error as AppError.UntrustedCertificate).info.changedFromPinned)
    }

    @Test
    fun unknownCertificate_reportsItsFingerprint() {
        val certificate = certificate()
        serve(certificate)

        val error = request { null }.error()

        assertEquals(
            CertificateInfoMapper.fingerprint(certificate.certificate),
            (error as AppError.UntrustedCertificate).info.sha256Fingerprint,
        )
    }

    @Test
    fun unknownCertificate_requestNeverReachesServer() {
        serve(certificate())

        request { null }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun pinnedCertificate_isAccepted() {
        val certificate = certificate()
        serve(certificate)
        val fingerprint = CertificateInfoMapper.fingerprint(certificate.certificate)

        assertEquals(200, request { host -> fingerprint.takeIf { host == "localhost" } }.getOrThrow())
    }

    @Test
    fun pinForOtherHost_isNotUsed() {
        val certificate = certificate()
        serve(certificate)
        val fingerprint = CertificateInfoMapper.fingerprint(certificate.certificate)

        val error = request { host -> fingerprint.takeIf { host == "cloud.example.com" } }.error()

        assertTrue(error is AppError.UntrustedCertificate)
    }

    @Test
    fun changedCertificate_reportsChange() {
        serve(certificate())
        val otherFingerprint = CertificateInfoMapper.fingerprint(certificate().certificate)

        val error = request { otherFingerprint }.error()

        assertTrue((error as AppError.UntrustedCertificate).info.changedFromPinned)
    }

    @Test
    fun pinnedCertificateWithOtherHostName_isAccepted() {
        val certificate = certificate(host = "cloud.example.com")
        serve(certificate)
        val fingerprint = CertificateInfoMapper.fingerprint(certificate.certificate)

        assertEquals(200, request { fingerprint }.getOrThrow())
    }
}
