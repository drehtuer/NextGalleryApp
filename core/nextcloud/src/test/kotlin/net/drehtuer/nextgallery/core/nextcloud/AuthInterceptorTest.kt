package net.drehtuer.nextgallery.core.nextcloud

import net.drehtuer.nextgallery.core.model.NextcloudAccount
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.UnknownServiceException

class AuthInterceptorTest {
    private val server = MockWebServer()
    private val otherServer = MockWebServer()

    private val expectedHeader = "Basic " + java.util.Base64.getEncoder().encodeToString("anna:secret".toByteArray())

    @After
    fun tearDown() {
        server.shutdown()
        otherServer.shutdown()
    }

    private fun module(serverUrl: String = server.url("/").toString().trimEnd('/')) = TestTls.module(
        credentials = { NextcloudAccount(serverUrl, "anna", "anna", "secret") },
        allowCleartext = true,
    )

    private fun fetch(client: okhttp3.OkHttpClient) =
        client.newCall(Request.Builder().url(server.url("/file")).build()).execute().close()

    @Test
    fun requestToTheAccountsServer_getsCredentials() {
        server.enqueue(MockResponse())

        fetch(module().authenticated)

        assertEquals(expectedHeader, server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun requestToTheAccountsServer_getsOcsHeader() {
        server.enqueue(MockResponse())

        fetch(module().authenticated)

        assertEquals("true", server.takeRequest().getHeader("OCS-APIRequest"))
    }

    @Test
    fun requestToAnotherPort_getsNoCredentials() {
        server.enqueue(MockResponse())

        fetch(module(otherServer.url("/").toString().trimEnd('/')).authenticated)

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun noAccount_sendsNoCredentials() {
        server.enqueue(MockResponse())
        val module = TestTls.module(allowCleartext = true)

        fetch(module.authenticated)

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun explicitHeaderToAnotherHost_isRemoved() {
        server.enqueue(MockResponse())
        val module = module(otherServer.url("/").toString().trimEnd('/'))
        val request = Request.Builder().url(server.url("/")).header("Authorization", "Basic x").build()

        module.authenticated.newCall(request).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun redirectToAnotherHost_dropsCredentials() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", otherServer.url("/elsewhere")))
        otherServer.enqueue(MockResponse())

        fetch(module().authenticated)

        assertEquals(expectedHeader, server.takeRequest().getHeader("Authorization"))
        assertNull(otherServer.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun redirectOnTheSameHost_keepsCredentials() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/moved")))
        server.enqueue(MockResponse())

        fetch(module().authenticated)

        server.takeRequest()
        assertEquals(expectedHeader, server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun noRedirectsClient_doesNotFollowARedirect() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", otherServer.url("/elsewhere")))

        fetch(module().noRedirects)

        assertEquals(0, otherServer.requestCount)
    }

    @Test
    fun plainHttp_isRefusedWithoutCleartext() {
        val module = TestTls.module(allowCleartext = false)

        assertThrows(UnknownServiceException::class.java) { fetch(module.base) }
    }
}
