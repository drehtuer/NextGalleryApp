package net.drehtuer.nextgallery.core.common

import java.io.IOException

/** Errors the UI can react to. New cases are added when a feature needs them. */
sealed interface AppError {
    data class Io(val message: String?) : AppError

    data object PermissionDenied : AppError

    data object NotFound : AppError

    /** An edit needs more memory than is available (D30). Nothing was changed. */
    data class InsufficientMemory(val requiredBytes: Long, val availableBytes: Long) : AppError

    data class Unknown(val cause: Throwable) : AppError

    companion object {
        fun from(throwable: Throwable): AppError = when (throwable) {
            is IOException -> Io(throwable.message)
            is SecurityException -> PermissionDenied
            else -> Unknown(throwable)
        }
    }
}
