package net.drehtuer.nextgallery.core.nextcloud

import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.util.concurrent.TimeUnit

/** A CA the test's "platform" trusts, and certificates for test servers. */
internal object TestTls {
    val trustedCa: HeldCertificate = HeldCertificate.Builder().certificateAuthority(0).commonName("Trusted CA").build()

    /** Stands in for the system's trust store: trusts only [trustedCa]. */
    val platformTrust = HandshakeCertificates.Builder().addTrustedCertificate(
        trustedCa.certificate,
    ).build().trustManager

    fun signedByTrustedCa(host: String = "localhost"): HeldCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName(host)
        .signedBy(trustedCa)
        .build()

    fun selfSigned(host: String = "localhost"): HeldCertificate = HeldCertificate.Builder()
        .commonName("Self signed")
        .addSubjectAlternativeName(host)
        .build()

    fun expiredSignedByTrustedCa(): HeldCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName("localhost")
        .validityInterval(1, TimeUnit.DAYS.toMillis(1))
        .signedBy(trustedCa)
        .build()

    fun MockWebServer.serve(certificate: HeldCertificate, vararg intermediates: HeldCertificate) {
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate, *intermediates.map { it.certificate }.toTypedArray())
            .build()
        useHttps(serverCertificates.sslSocketFactory(), false)
    }

    /** A module for tests: the real trust handling on top of the test's "platform". */
    fun module(
        pins: CertificatePinSource = CertificatePinSource { null },
        credentials: CredentialsProvider = CredentialsProvider { null },
        allowCleartext: Boolean = false,
    ) = NextcloudHttpModule(
        config = NextcloudHttpConfig("NextGalleryApp/test", allowCleartext),
        credentials = credentials,
        pins = pins,
        platformTrustManager = platformTrust,
    )
}
