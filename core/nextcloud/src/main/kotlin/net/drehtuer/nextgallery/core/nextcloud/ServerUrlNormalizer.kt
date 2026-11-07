package net.drehtuer.nextgallery.core.nextcloud

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The user entered something that cannot be used as a server address. */
class InvalidServerUrlException(message: String) : IllegalArgumentException(message)

/** Turns what the user typed into the server's base URL (https only, unless [allowCleartext]; D19). */
class ServerUrlNormalizer(private val allowCleartext: Boolean) {
    fun normalize(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) reject("Empty address")
        val withScheme = if ("://" in trimmed) trimmed else "$HTTPS://$trimmed"
        if (!isAllowedScheme(withScheme.substringBefore("://").lowercase())) {
            reject("Unsupported scheme")
        }
        val url = withScheme.toHttpUrlOrNull() ?: reject("Not a URL")
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            reject("Address must not contain credentials")
        }
        val path = url.encodedPath.trimEnd('/').removeSuffix(INDEX_PHP).trimEnd('/')
        return url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString().trimEnd('/') + path
    }

    private fun reject(reason: String): Nothing = throw InvalidServerUrlException(reason)

    private fun isAllowedScheme(scheme: String) = scheme == HTTPS || (scheme == HTTP && allowCleartext)

    private companion object {
        const val HTTPS = "https"
        const val HTTP = "http"
        const val INDEX_PHP = "/index.php"
    }
}
