package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.model.CertificateInfo
import java.security.cert.CertificateException

/** The server's certificate is not trusted; the user can look at [info] and decide (D32). */
class UntrustedCertificateException(val info: CertificateInfo) :
    CertificateException("Untrusted certificate for ${info.host}")
