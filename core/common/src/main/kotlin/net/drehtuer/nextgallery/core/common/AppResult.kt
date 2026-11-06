package net.drehtuer.nextgallery.core.common

/** Result of a command in the data layer. Repositories catch exceptions and return [Failure] instead. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}
