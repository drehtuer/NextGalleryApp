package net.drehtuer.nextgallery.core.nextcloud

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OcsClientTest {
    private val server = MockWebServer()
    private val otherServer = MockWebServer()
    private val client = OcsClient(TestTls.module(allowCleartext = true).unauthenticatedNoRedirects)

    private val serverUrl get() = server.url("/").toString().trimEnd('/')

    @After
    fun tearDown() {
        server.shutdown()
        otherServer.shutdown()
    }

    @Test
    fun userId_readsTheIdFromTheResponse() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"ocs":{"data":{"id":"anna-id"}}}"""))

        assertEquals("anna-id", client.userId(serverUrl, "anna", "pw"))
    }

    @Test
    fun userId_sendsCredentialsAndOcsHeader() = runBlocking<Unit> {
        server.enqueue(MockResponse().setBody("""{"ocs":{"data":{"id":"anna-id"}}}"""))

        client.userId(serverUrl, "anna", "pw")

        val request = server.takeRequest()
        assertEquals("/ocs/v2.php/cloud/user?format=json", request.path)
        assertTrue(request.getHeader("Authorization").orEmpty().startsWith("Basic "))
        assertEquals("true", request.getHeader("OCS-APIRequest"))
    }

    @Test
    fun userId_redirect_isNotFollowedAndFails() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", otherServer.url("/x")))

        assertThrows(NextcloudHttpException::class.java) { runBlocking { client.userId(serverUrl, "anna", "pw") } }
        assertEquals(0, otherServer.requestCount)
    }

    @Test
    fun userId_invalidBody_failsWithIoException() {
        server.enqueue(MockResponse().setBody("nope"))

        assertThrows(IOException::class.java) { runBlocking { client.userId(serverUrl, "anna", "pw") } }
    }

    @Test
    fun userId_incompleteBody_failsWithIoException() {
        server.enqueue(MockResponse().setBody("""{"ocs":{}}"""))

        assertThrows(IOException::class.java) { runBlocking { client.userId(serverUrl, "anna", "pw") } }
    }

    @Test
    fun revokeAppPassword_sendsDelete() = runBlocking<Unit> {
        server.enqueue(MockResponse())

        client.revokeAppPassword(serverUrl, "anna", "pw")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/ocs/v2.php/core/apppassword", request.path)
    }

    @Test
    fun revokeAppPassword_serverError_fails() {
        server.enqueue(MockResponse().setResponseCode(500))

        assertThrows(NextcloudHttpException::class.java) {
            runBlocking { client.revokeAppPassword(serverUrl, "anna", "pw") }
        }
    }
}
