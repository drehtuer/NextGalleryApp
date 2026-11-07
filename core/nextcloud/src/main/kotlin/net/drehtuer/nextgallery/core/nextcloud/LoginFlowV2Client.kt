package net.drehtuer.nextgallery.core.nextcloud

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.HttpURLConnection

/** A running login flow. The poll token is as good as the password for a while, so it never appears in [toString]. */
data class LoginFlowSession(val loginUrl: String, val pollEndpoint: String, val token: String) {
    override fun toString(): String =
        "LoginFlowSession(loginUrl=$loginUrl, pollEndpoint=$pollEndpoint, token=<redacted>)"
}

/** What the server returns once the user has signed in. */
data class LoginFlowResult(val serverUrl: String, val loginName: String, val appPassword: String) {
    override fun toString(): String =
        "LoginFlowResult(serverUrl=$serverUrl, loginName=$loginName, appPassword=<redacted>)"
}

/**
 * Nextcloud Login Flow v2. The URLs in the responses come from the network and are opened in a browser or receive the
 * token, so only https is accepted, or http if [allowCleartext] (D19).
 */
class LoginFlowV2Client(
    private val client: OkHttpClient,
    private val allowCleartext: Boolean,
    private val pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MILLIS,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun start(serverUrl: String): LoginFlowSession = withContext(io) {
        val request = Request.Builder().url("$serverUrl/index.php/login/v2").post(EMPTY_BODY).build()
        client.newCall(request).await().use { response ->
            response.requireSuccess()
            parseSession(response.body.string())
        }
    }

    /** Asks until the user has signed in. Fails with [LoginExpiredException] after the timeout (20 minutes). */
    suspend fun poll(session: LoginFlowSession): LoginFlowResult {
        repeat((timeoutMillis / pollIntervalMillis).toInt().coerceAtLeast(1)) {
            pollOnce(session)?.let { return it }
            delay(pollIntervalMillis)
        }
        throw LoginExpiredException()
    }

    private suspend fun pollOnce(session: LoginFlowSession): LoginFlowResult? = withContext(io) {
        val body = FormBody.Builder().add("token", session.token).build()
        val request = Request.Builder().url(session.pollEndpoint).post(body).build()
        client.newCall(request).await().use { response ->
            if (response.code == HttpURLConnection.HTTP_NOT_FOUND) return@withContext null
            response.requireSuccess()
            parseResult(response.body.string())
        }
    }

    private fun parseSession(body: String): LoginFlowSession = parsing {
        val root = Json.parseToJsonElement(body).jsonObject
        val poll = root.getValue("poll").jsonObject
        LoginFlowSession(
            loginUrl = requireAllowedUrl(root.getValue("login").jsonPrimitive.content),
            pollEndpoint = requireAllowedUrl(poll.getValue("endpoint").jsonPrimitive.content),
            token = poll.getValue("token").jsonPrimitive.content,
        )
    }

    private fun parseResult(body: String): LoginFlowResult = parsing {
        val root = Json.parseToJsonElement(body).jsonObject
        LoginFlowResult(
            serverUrl = requireAllowedUrl(root.getValue("server").jsonPrimitive.content).trimEnd('/'),
            loginName = root.getValue("loginName").jsonPrimitive.content,
            appPassword = root.getValue("appPassword").jsonPrimitive.content,
        )
    }

    private fun requireAllowedUrl(url: String): String {
        val scheme = url.toHttpUrlOrNull()?.scheme
        if (scheme != "https" && !(scheme == "http" && allowCleartext)) throw InvalidServerUrlException("Unsafe URL")
        return url
    }

    private inline fun <T> parsing(block: () -> T): T = try {
        block()
    } catch (e: InvalidServerUrlException) {
        throw e
    } catch (e: SerializationException) {
        throw IOException("The server sent an invalid response", e)
    } catch (e: NoSuchElementException) {
        throw IOException("The server sent an incomplete response", e)
    } catch (e: IllegalArgumentException) {
        throw IOException("The server sent an invalid response", e)
    }

    private companion object {
        const val DEFAULT_POLL_INTERVAL_MILLIS = 2_000L
        const val DEFAULT_TIMEOUT_MILLIS = 20 * 60 * 1_000L
        val EMPTY_BODY = ByteArray(0).toRequestBody()
    }
}
