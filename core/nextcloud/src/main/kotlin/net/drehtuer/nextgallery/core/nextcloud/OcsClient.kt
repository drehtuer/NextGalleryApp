package net.drehtuer.nextgallery.core.nextcloud

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The OCS calls around the login. The credentials go on the request here, because they are not (or no longer) in the
 * account store; [client] must not follow redirects, so they can't reach another host.
 */
class OcsClient(private val client: OkHttpClient, private val io: CoroutineDispatcher = Dispatchers.IO) {
    /** The user id for the DAV paths, which can differ from the login name. */
    suspend fun userId(serverUrl: String, loginName: String, appPassword: String): String = withContext(io) {
        val url = serverUrl.toHttpUrl().newBuilder()
            .addPathSegments("ocs/v2.php/cloud/user")
            .addQueryParameter("format", "json")
            .build()
        client.newCall(request(url, loginName, appPassword).get().build()).await().use { response ->
            response.requireSuccess()
            parseUserId(response.bodyAsLimitedString())
        }
    }

    /** Ends the app password on the server. Has a short limit, because the user is waiting to be logged out. */
    suspend fun revokeAppPassword(serverUrl: String, loginName: String, appPassword: String) = withContext(io) {
        val url = serverUrl.toHttpUrl().newBuilder().addPathSegments("ocs/v2.php/core/apppassword").build()
        client.newBuilder().callTimeout(REVOKE_TIMEOUT_SECONDS, TimeUnit.SECONDS).build()
            .newCall(request(url, loginName, appPassword).delete().build())
            .await()
            .use { it.requireSuccess() }
    }

    private fun parseUserId(body: String): String = try {
        Json.parseToJsonElement(body).jsonObject
            .getValue("ocs").jsonObject
            .getValue("data").jsonObject
            .getValue("id").jsonPrimitive.content
    } catch (e: SerializationException) {
        throw IOException("The server sent an invalid response", e)
    } catch (e: NoSuchElementException) {
        throw IOException("The server sent an incomplete response", e)
    } catch (e: IllegalArgumentException) {
        throw IOException("The server sent an invalid response", e)
    }

    private fun request(url: HttpUrl, loginName: String, appPassword: String): Request.Builder = Request.Builder()
        .url(url)
        .header(AuthInterceptor.HEADER_AUTHORIZATION, AuthInterceptor.basicAuth(loginName, appPassword))
        .header(AuthInterceptor.HEADER_OCS_API_REQUEST, "true")

    private companion object {
        const val REVOKE_TIMEOUT_SECONDS = 10L
    }
}
