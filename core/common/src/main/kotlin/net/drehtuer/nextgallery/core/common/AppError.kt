package net.drehtuer.nextgallery.core.common

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
