package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.model.NextcloudAccount
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/** The signed-in account, read when a request goes out. */
fun interface CredentialsProvider {
    fun current(): NextcloudAccount?
}

/**
 * Adds the credentials, but only to requests for the account's own server (scheme, host and port). It runs as a network
 * interceptor, so it decides again for each redirect: the password can't follow a redirect to another host.
 */
class AuthInterceptor(private val credentials: CredentialsProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val account = credentials.current()
        val server = account?.serverUrl?.toHttpUrlOrNull()
        if (account == null || server == null || !request.url.isSameOriginAs(server)) {
            return chain.proceed(request.newBuilder().removeHeader(HEADER_AUTHORIZATION).build())
        }
        return chain.proceed(
            request.newBuilder()
                .header(HEADER_AUTHORIZATION, basicAuth(account.loginName, account.appPassword))
                .header(HEADER_OCS_API_REQUEST, "true")
                .build(),
        )
    }

    private fun HttpUrl.isSameOriginAs(other: HttpUrl) =
        scheme == other.scheme && host == other.host && port == other.port

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_OCS_API_REQUEST = "OCS-APIRequest"

        fun basicAuth(loginName: String, password: String): String =
            Credentials.basic(loginName, password, Charsets.UTF_8)
    }
}
