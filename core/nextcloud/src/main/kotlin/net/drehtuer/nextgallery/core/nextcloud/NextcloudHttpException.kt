package net.drehtuer.nextgallery.core.nextcloud

import java.io.IOException

/** The server answered with a status the operation cannot use. */
class NextcloudHttpException(val code: Int) : IOException("HTTP $code")

/** The user did not finish signing in before the login flow ran out. */
class LoginExpiredException : IOException("The login flow expired")

/** The response is larger than anything the operation can need; reading on could run out of memory. */
class ResponseTooLargeException(cause: Throwable? = null) : IOException("The server sent too much data", cause)

/** A login response points to another server than the one the user entered. */
class ServerMismatchException : IOException("The login response is for another server")
