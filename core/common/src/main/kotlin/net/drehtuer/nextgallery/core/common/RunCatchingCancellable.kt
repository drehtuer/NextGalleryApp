package net.drehtuer.nextgallery.core.common

import kotlin.coroutines.cancellation.CancellationException

/** Like [runCatching], but never swallows [CancellationException], so coroutine cancellation keeps working. */
inline fun <T> runCatchingCancellable(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (
    @Suppress("TooGenericExceptionCaught") e: Exception,
) {
    AppResult.Failure(AppError.from(e))
}
