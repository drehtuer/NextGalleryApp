package net.drehtuer.nextgallery.core.nextcloud

import java.io.IOException

/** The server answered with a status the operation cannot use. */
class NextcloudHttpException(val code: Int) : IOException("HTTP $code")

/** The user did not finish signing in before the login flow ran out. */
class LoginExpiredException : IOException("The login flow expired")
