package net.drehtuer.nextgallery.core.nextcloud

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Runs the call without blocking a thread; cancelling the coroutine cancels the call. */
internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, value, _ -> value.close() }
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        },
    )
}

internal fun Response.requireSuccess() {
    if (!isSuccessful) throw NextcloudHttpException(code)
}

/** A login or OCS response has a few short fields; this is far more than any of them needs. */
internal const val MAX_JSON_BYTES = 1024L * 1024L

/** The body as text. Reads no more than [maxBytes] (after decompression), and fails if there is more. */
internal fun Response.bodyAsLimitedString(maxBytes: Long = MAX_JSON_BYTES): String {
    val limited = peekBody(maxBytes + 1)
    if (limited.contentLength() > maxBytes) throw ResponseTooLargeException()
    return limited.string()
}
