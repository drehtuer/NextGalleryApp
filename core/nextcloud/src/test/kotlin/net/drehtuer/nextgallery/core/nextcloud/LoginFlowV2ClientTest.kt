package net.drehtuer.nextgallery.core.nextcloud

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class LoginFlowV2ClientTest {
    private val server = MockWebServer()
    private val client =
        LoginFlowV2Client(OkHttpClient(), allowCleartext = true, pollIntervalMillis = 1, timeoutMillis = 3)

    private val session get() = LoginFlowSession("https://l", server.url("/login/v2/poll").toString(), "poll-token")

    @After
    fun tearDown() = server.shutdown()

    private fun sessionJson(
        login: String = "https://cloud.example.com/login",
        endpoint: String = "https://cloud.example.com/poll",
    ) = """{"poll":{"token":"t0k3n","endpoint":"$endpoint"},"login":"$login"}"""

    @Test
    fun start_postsToTheLoginEndpoint() = runBlocking {
        server.enqueue(MockResponse().setBody(sessionJson()))

        client.start(server.url("/nc").toString())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/nc/index.php/login/v2", request.path)
    }

    @Test
    fun start_returnsLoginUrlEndpointAndToken() = runBlocking {
        server.enqueue(MockResponse().setBody(sessionJson()))

        val result = client.start(server.url("/").toString().trimEnd('/'))

        assertEquals(
            LoginFlowSession("https://cloud.example.com/login", "https://cloud.example.com/poll", "t0k3n"),
            result,
        )
    }

    @Test
    fun start_javascriptLoginUrl_isRefused() {
        server.enqueue(MockResponse().setBody(sessionJson(login = "javascript:alert(1)")))

        assertThrows(InvalidServerUrlException::class.java) {
            runBlocking { client.start(server.url("/").toString().trimEnd('/')) }
        }
    }

    @Test
    fun start_httpPollEndpointWithoutCleartext_isRefused() {
        val strict = LoginFlowV2Client(OkHttpClient(), allowCleartext = false)
        server.enqueue(MockResponse().setBody(sessionJson(endpoint = "http://cloud.example.com/poll")))

        assertThrows(InvalidServerUrlException::class.java) {
            runBlocking { strict.start(server.url("/").toString().trimEnd('/')) }
        }
    }

    @Test
    fun start_serverError_failsWithItsCode() {
        server.enqueue(MockResponse().setResponseCode(500))

        val error = assertThrows(NextcloudHttpException::class.java) {
            runBlocking { client.start(server.url("/").toString().trimEnd('/')) }
        }

        assertEquals(500, error.code)
    }

    @Test
    fun start_notJson_failsWithIoException() {
        server.enqueue(MockResponse().setBody("<html>"))

        assertThrows(IOException::class.java) {
            runBlocking { client.start(server.url("/").toString().trimEnd('/')) }
        }
    }

    @Test
    fun start_incompleteJson_failsWithIoException() {
        server.enqueue(MockResponse().setBody("""{"login":"https://a"}"""))

        assertThrows(IOException::class.java) {
            runBlocking { client.start(server.url("/").toString().trimEnd('/')) }
        }
    }

    @Test
    fun poll_success_returnsCredentials() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"server":"https://cloud.example.com/","loginName":"anna","appPassword":"pw"}"""),
        )

        val result = client.poll(session)

        assertEquals(LoginFlowResult("https://cloud.example.com", "anna", "pw"), result)
    }

    @Test
    fun poll_sendsTheTokenInTheBody() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"server":"https://c","loginName":"a","appPassword":"p"}"""))

        client.poll(session)

        assertEquals("token=poll-token", server.takeRequest().body.readUtf8())
    }

    @Test
    fun poll_pendingThenSuccess_keepsAsking() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setBody("""{"server":"https://c","loginName":"a","appPassword":"p"}"""))

        assertEquals("a", client.poll(session).loginName)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun poll_stillPendingAtTheEnd_failsAsExpired() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(404)) }

        assertThrows(LoginExpiredException::class.java) { runBlocking { client.poll(session) } }
    }

    @Test
    fun poll_serverOverHttpWithoutCleartext_isRefused() {
        val strict = LoginFlowV2Client(OkHttpClient(), allowCleartext = false)
        server.enqueue(MockResponse().setBody("""{"server":"http://c","loginName":"a","appPassword":"p"}"""))

        assertThrows(InvalidServerUrlException::class.java) { runBlocking { strict.poll(session) } }
    }

    @Test
    fun session_toStringHidesTheToken() {
        assertFalse(LoginFlowSession("https://l", "https://e", "t0k3n").toString().contains("t0k3n"))
    }

    @Test
    fun result_toStringHidesThePassword() {
        assertFalse(LoginFlowResult("https://s", "anna", "s3cret").toString().contains("s3cret"))
    }
}
