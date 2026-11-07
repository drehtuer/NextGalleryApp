package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.model.CertificateInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import javax.net.ssl.SSLHandshakeException

class NextcloudErrorMapperTest {
    @Test
    fun map_401_isAuth() {
        assertEquals(AppError.Auth, NextcloudErrorMapper.map(NextcloudHttpException(401)))
    }

    @Test
    fun map_403_isForbidden() {
        assertEquals(AppError.Forbidden, NextcloudErrorMapper.map(NextcloudHttpException(403)))
    }

    @Test
    fun map_404_isNotFound() {
        assertEquals(AppError.NotFound, NextcloudErrorMapper.map(NextcloudHttpException(404)))
    }

    @Test
    fun map_412_isConflict() {
        assertEquals(AppError.Conflict, NextcloudErrorMapper.map(NextcloudHttpException(412)))
    }

    @Test
    fun map_405_isConflict() {
        assertEquals(AppError.Conflict, NextcloudErrorMapper.map(NextcloudHttpException(405)))
    }

    @Test
    fun map_500_isServerWithCode() {
        assertEquals(AppError.Server(500), NextcloudErrorMapper.map(NextcloudHttpException(500)))
    }

    @Test
    fun map_ioException_isNetwork() {
        assertEquals(AppError.Network("gone"), NextcloudErrorMapper.map(IOException("gone")))
    }

    @Test
    fun map_untrustedCertificateAsCause_isUntrustedCertificate() {
        val info = CertificateInfo("h", "s", "i", 1, 2, "AA")
        val error = SSLHandshakeException("failed").apply { initCause(UntrustedCertificateException(info)) }

        assertEquals(AppError.UntrustedCertificate(info), NextcloudErrorMapper.map(error))
    }

    @Test
    fun map_untrustedCertificateAsSuppressed_isUntrustedCertificate() {
        val info = CertificateInfo("h", "s", "i", 1, 2, "AA")
        val error = IOException("first address unreachable").apply {
            addSuppressed(SSLHandshakeException("failed").apply { initCause(UntrustedCertificateException(info)) })
        }

        assertEquals(AppError.UntrustedCertificate(info), NextcloudErrorMapper.map(error))
    }

    @Test
    fun map_expiredLogin_isLoginExpired() {
        assertEquals(AppError.LoginExpired, NextcloudErrorMapper.map(LoginExpiredException()))
    }

    @Test
    fun map_invalidUrl_isInvalidServerUrl() {
        assertEquals(AppError.InvalidServerUrl, NextcloudErrorMapper.map(InvalidServerUrlException("x")))
    }

    @Test
    fun map_otherError_isUnknown() {
        val error = IllegalStateException("x")

        assertEquals(AppError.Unknown(error), NextcloudErrorMapper.map(error))
    }
}
