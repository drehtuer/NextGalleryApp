package net.drehtuer.nextgallery.core.model

/**
 * A server certificate, for the warning dialog and the list of accepted certificates (D32).
 * [pinnedFingerprint] is the certificate accepted earlier for this host, if this one differs from it.
 */
data class CertificateInfo(
    val host: String,
    val subject: String,
    val issuer: String,
    val notBeforeMillis: Long,
    val notAfterMillis: Long,
    val sha256Fingerprint: String,
    val pinnedFingerprint: String? = null,
) {
    val changedFromPinned: Boolean get() = pinnedFingerprint != null
}
