package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.common.AppError
import java.io.IOException
import java.net.HttpURLConnection

object NextcloudErrorMapper {
    private const val PRECONDITION_FAILED = 412

    fun map(error: Throwable): AppError {
        val untrusted = findUntrusted(error, mutableSetOf())
        return when {
            untrusted != null -> AppError.UntrustedCertificate(untrusted.info)
            error is NextcloudHttpException -> mapStatus(error.code)
            error is LoginExpiredException -> AppError.LoginExpired
            error is InvalidServerUrlException -> AppError.InvalidServerUrl
            error is IOException -> AppError.Network(error.message)
            else -> AppError.from(error)
        }
    }

    /**
     * Looks through causes and suppressed exceptions: if a server has several addresses, OkHttp throws the failure of
     * the first one (e.g. an unreachable IPv6 address) and adds the TLS failure of the next one as suppressed.
     */
    private fun findUntrusted(error: Throwable, seen: MutableSet<Throwable>): UntrustedCertificateException? = when {
        error is UntrustedCertificateException -> error
        !seen.add(error) -> null
        else -> (listOfNotNull(error.cause) + error.suppressed).firstNotNullOfOrNull { findUntrusted(it, seen) }
    }

    private fun mapStatus(code: Int): AppError = when (code) {
        HttpURLConnection.HTTP_UNAUTHORIZED -> AppError.Auth
        HttpURLConnection.HTTP_FORBIDDEN -> AppError.Forbidden
        HttpURLConnection.HTTP_NOT_FOUND -> AppError.NotFound
        HttpURLConnection.HTTP_BAD_METHOD, PRECONDITION_FAILED -> AppError.Conflict
        else -> AppError.Server(code)
    }
}
