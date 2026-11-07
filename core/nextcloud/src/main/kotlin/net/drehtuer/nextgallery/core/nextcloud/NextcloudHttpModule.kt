package net.drehtuer.nextgallery.core.nextcloud

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** [allowCleartext] is only true in debug builds (D19). */
data class NextcloudHttpConfig(val userAgent: String, val allowCleartext: Boolean)

/** Forgets the open connections, so that the next request goes through the certificate checks again. */
fun interface ConnectionEvictor {
    fun evictConnections()
}

/**
 * The HTTP clients. All remote traffic goes through them, so all of it follows the same certificate rules (D32).
 *
 * - [base]: no credentials, for the login flow.
 * - [authenticated]: adds the account's credentials for its own server, for reading.
 * - [noRedirects]: as [authenticated], for changes on the server, which must never be repeated somewhere else.
 * - [unauthenticatedNoRedirects]: as [base], for requests that carry credentials the account store does not hold.
 */
class NextcloudHttpModule(
    config: NextcloudHttpConfig,
    credentials: CredentialsProvider,
    pins: CertificatePinSource,
    val downloadProgress: DownloadProgressTracker = DownloadProgressTracker(),
    platformTrustManager: X509TrustManager = systemTrustManager(),
    platformHostnameVerifier: HostnameVerifier = OkHttpClient().hostnameVerifier,
) : ConnectionEvictor {
    private val trustManager = PinningTrustManager(platformTrustManager, pins)
    private val sslContext = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }

    val base: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .sslSocketFactory(sslContext.socketFactory, trustManager)
        .hostnameVerifier(PinningHostnameVerifier(platformHostnameVerifier, pins))
        .connectionSpecs(connectionSpecs(config.allowCleartext))
        .followSslRedirects(false)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", config.userAgent).build())
        }
        .build()

    val unauthenticatedNoRedirects: OkHttpClient = base.newBuilder().followRedirects(false).build()

    val authenticated: OkHttpClient = base.newBuilder()
        .addNetworkInterceptor(AuthInterceptor(credentials))
        .addNetworkInterceptor(downloadProgress)
        .build()

    val noRedirects: OkHttpClient = authenticated.newBuilder().followRedirects(false).build()

    /** Also drops resumable TLS sessions: a resumed session would skip the trust manager. */
    override fun evictConnections() {
        base.connectionPool.evictAll()
        val sessions = sslContext.clientSessionContext
        sessions.ids.toList().forEach { sessions.getSession(it)?.invalidate() }
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 60L

        fun connectionSpecs(allowCleartext: Boolean): List<ConnectionSpec> = if (allowCleartext) {
            listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT)
        } else {
            listOf(ConnectionSpec.MODERN_TLS)
        }

        fun systemTrustManager(): X509TrustManager {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            return factory.trustManagers.filterIsInstance<X509TrustManager>().single()
        }
    }
}
