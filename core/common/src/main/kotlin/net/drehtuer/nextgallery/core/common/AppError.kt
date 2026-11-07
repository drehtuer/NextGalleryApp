package net.drehtuer.nextgallery.core.common

import net.drehtuer.nextgallery.core.model.CertificateInfo
import java.io.IOException

/** Errors the UI can react to. New cases are added when a feature needs them. */
sealed interface AppError {
    data class Io(val message: String?) : AppError

    data object PermissionDenied : AppError

    data object NotFound : AppError

    /** An edit needs more memory than is available (D30). Nothing was changed. */
    data class InsufficientMemory(val requiredBytes: Long, val availableBytes: Long) : AppError

    /** Overwriting an image failed and so did putting the original back. See [recoveredAsCopy]. */
    data class OriginalDamaged(val recoveredAsCopy: Boolean) : AppError

    data class Unknown(val cause: Throwable) : AppError

    /** The server could not be reached. */
    data class Network(val message: String?) : AppError

    /** The server rejected the credentials (401): the app password no longer works. */
    data object Auth : AppError

    /** A remote operation was requested without an account (D35); nothing was sent. */
    data object NotLoggedIn : AppError

    data object NotFound : AppError

    /** The account may not do this, e.g. a read-only share (403). */
    data object Forbidden : AppError

    /** Something already exists at the target (412 or 405). */
    data object Conflict : AppError

    data class Server(val code: Int) : AppError

    /** The server presents a certificate that needs the user's decision (D32). */
    data class UntrustedCertificate(val info: CertificateInfo) : AppError

    /** A connection to a local address failed and the app may not use the local network (D39). */
    data object LocalNetworkBlocked : AppError

    /** The address the user entered is not a usable https address. */
    data object InvalidServerUrl : AppError

    /** The user did not finish signing in in time. */
    data object LoginExpired : AppError

    /** A file or folder name the server cannot have. */
    data object InvalidName : AppError

    companion object {
        fun from(throwable: Throwable): AppError = when (throwable) {
            is OriginalDamagedException -> OriginalDamaged(throwable.recoveredAsCopy)
            is IOException -> Io(throwable.message)
            is SecurityException -> PermissionDenied
            else -> Unknown(throwable)
        }
    }
}

/** See [AppError.OriginalDamaged]. */
class OriginalDamagedException(val recoveredAsCopy: Boolean, cause: Throwable?) :
    IOException("The original image could not be restored", cause)
