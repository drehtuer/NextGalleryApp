package net.drehtuer.nextgallery.core.nextcloud

import kotlinx.coroutines.runBlocking
import net.drehtuer.nextgallery.core.model.NextcloudAccount
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class WebDavClientTest {
    private val server = MockWebServer()
    private val otherServer = MockWebServer()
    private val account get() = NextcloudAccount(server.url("/").toString().trimEnd('/'), "anna", "anna", "pw")
    private val module = TestTls.module(credentials = { account }, allowCleartext = true)
    private val client = WebDavClient(module.authenticated, module.noRedirects)

    private val multistatus = """<?xml version="1.0"?>
        <d:multistatus xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns">
          <d:response><d:href>/remote.php/dav/files/anna/Photos/</d:href>
            <d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype>
              <oc:permissions>RGDNVCK</oc:permissions></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>
          </d:response>
          <d:response><d:href>/remote.php/dav/files/anna/Photos/a.jpg</d:href>
            <d:propstat><d:prop><d:resourcetype/><d:getcontenttype>image/jpeg</d:getcontenttype></d:prop>
              <d:status>HTTP/1.1 200 OK</d:status></d:propstat>
          </d:response>
        </d:multistatus>"""

    @After
    fun tearDown() {
        server.shutdown()
        otherServer.shutdown()
    }

    private fun assertFailsWithCode(code: Int, block: suspend () -> Unit) {
        server.enqueue(MockResponse().setResponseCode(code))

        val error = assertThrows(NextcloudHttpException::class.java) { runBlocking { block() } }

        assertEquals(code, error.code)
    }

    @Test
    fun list_sendsPropfindWithDepthOne() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(207).setBody(multistatus))

        client.list(account, "/Photos")

        val request = server.takeRequest()
        assertEquals("PROPFIND", request.method)
        assertEquals("1", request.getHeader("Depth"))
        assertEquals("/remote.php/dav/files/anna/Photos", request.path)
        assertTrue(request.body.readUtf8().contains("oc:permissions"))
    }

    @Test
    fun list_separatesTheFolderFromItsChildren() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(multistatus))

        val folder = client.list(account, "/Photos")

        assertEquals("RGDNVCK", folder.self?.permissions)
        assertEquals(listOf("/Photos/a.jpg"), folder.children.map { it.path })
    }

    @Test
    fun list_401_failsWithCode() = assertFailsWithCode(401) { client.list(account, "/") }

    @Test
    fun list_404_failsWithCode() = assertFailsWithCode(404) { client.list(account, "/gone") }

    @Test
    fun stat_sendsDepthZero() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(multistatus))

        client.stat(account, "/Photos/a.jpg")

        assertEquals("0", server.takeRequest().getHeader("Depth"))
    }

    @Test
    fun stat_returnsTheItem() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(multistatus))

        assertEquals("a.jpg", client.stat(account, "/Photos/a.jpg")?.name)
    }

    @Test
    fun stat_otherPathInResponse_isNull() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(multistatus))

        assertNull(client.stat(account, "/Photos/b.jpg"))
    }

    @Test
    fun download_writesTheBodyToTheSink() = runBlocking {
        server.enqueue(MockResponse().setBody("image bytes"))
        val sink = ByteArrayOutputStream()

        client.download(account, "/Photos/a.jpg", sink)

        assertEquals("image bytes", sink.toString())
    }

    @Test
    fun download_reportsProgress() = runBlocking {
        server.enqueue(MockResponse().setBody("12345"))
        var last = 0L to 0L

        client.download(account, "/a.jpg", ByteArrayOutputStream()) { read, total -> last = read to total }

        assertEquals(5L to 5L, last)
    }

    @Test
    fun download_401_failsWithCode() =
        assertFailsWithCode(401) { client.download(account, "/a.jpg", ByteArrayOutputStream()) }

    @Test
    fun download_404_failsWithCode() =
        assertFailsWithCode(404) { client.download(account, "/a.jpg", ByteArrayOutputStream()) }

    @Test
    fun move_byDefault_sendsOverwriteF() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))

        client.move(account, "/a.jpg", "/b/a.jpg")

        assertEquals("F", server.takeRequest().getHeader("Overwrite"))
    }

    @Test
    fun move_sendsMoveWithEncodedDestination() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))

        client.move(account, "/a.jpg", "/My Folder/a.jpg")

        val request = server.takeRequest()
        assertEquals("MOVE", request.method)
        assertEquals("/remote.php/dav/files/anna/a.jpg", request.path)
        assertEquals(
            server.url("/remote.php/dav/files/anna/My%20Folder/a.jpg").toString(),
            request.getHeader("Destination"),
        )
    }

    @Test
    fun move_withOverwrite_sendsOverwriteT() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.move(account, "/a.jpg", "/b/a.jpg", overwrite = true)

        assertEquals("T", server.takeRequest().getHeader("Overwrite"))
    }

    @Test
    fun move_existingTarget_failsWith412() = assertFailsWithCode(412) { client.move(account, "/a.jpg", "/b/a.jpg") }

    @Test
    fun move_readOnlyFolder_failsWith403() = assertFailsWithCode(403) { client.move(account, "/a.jpg", "/b/a.jpg") }

    @Test
    fun move_401_failsWithCode() = assertFailsWithCode(401) { client.move(account, "/a.jpg", "/b/a.jpg") }

    @Test
    fun move_404_failsWithCode() = assertFailsWithCode(404) { client.move(account, "/a.jpg", "/b/a.jpg") }

    @Test
    fun move_redirect_isNotFollowedAndFails() {
        server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", otherServer.url("/x")))

        assertThrows(NextcloudHttpException::class.java) {
            runBlocking { client.move(account, "/a.jpg", "/b/a.jpg") }
        }
        assertEquals(0, otherServer.requestCount)
    }

    @Test
    fun createFolder_sendsMkcol() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))

        client.createFolder(account, "/New Folder")

        val request = server.takeRequest()
        assertEquals("MKCOL", request.method)
        assertEquals("/remote.php/dav/files/anna/New%20Folder", request.path)
    }

    @Test
    fun createFolder_exists_failsWith405() = assertFailsWithCode(405) { client.createFolder(account, "/a") }

    @Test
    fun createFolder_readOnly_failsWith403() = assertFailsWithCode(403) { client.createFolder(account, "/a") }

    @Test
    fun delete_sendsDelete() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.delete(account, "/a.jpg")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/remote.php/dav/files/anna/a.jpg", request.path)
    }

    @Test
    fun delete_401_failsWithCode() = assertFailsWithCode(401) { client.delete(account, "/a.jpg") }

    @Test
    fun delete_404_failsWithCode() = assertFailsWithCode(404) { client.delete(account, "/a.jpg") }

    @Test
    fun delete_readOnly_failsWith403() = assertFailsWithCode(403) { client.delete(account, "/a.jpg") }

    @Test
    fun delete_redirect_isNotFollowedAndFails() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", otherServer.url("/x")))

        assertThrows(NextcloudHttpException::class.java) { runBlocking { client.delete(account, "/a.jpg") } }
        assertEquals(0, otherServer.requestCount)
    }

    @Test
    fun listTrash_readsTheEntries() = runBlocking {
        val body = """<?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:nc="http://nextcloud.org/ns">
              <d:response><d:href>/remote.php/dav/trashbin/anna/trash/a.jpg.d5</d:href>
                <d:propstat><d:prop><nc:trashbin-filename>a.jpg</nc:trashbin-filename>
                  <nc:trashbin-original-location>Photos/a.jpg</nc:trashbin-original-location>
                  <nc:trashbin-deletion-time>5</nc:trashbin-deletion-time></d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status></d:propstat>
              </d:response>
            </d:multistatus>"""
        server.enqueue(MockResponse().setResponseCode(207).setBody(body))

        val items = client.listTrash(account)

        assertEquals("/remote.php/dav/trashbin/anna/trash", server.takeRequest().path)
        assertEquals(listOf(TrashItem("a.jpg.d5", "a.jpg", "/Photos/a.jpg", 5, null)), items)
    }

    @Test
    fun restore_movesTheEntryToTheRestoreFolder() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))

        client.restore(account, TrashItem("a.jpg.d5", "a.jpg", "/Photos/a.jpg", 5, 42))

        val request = server.takeRequest()
        assertEquals("MOVE", request.method)
        assertEquals("/remote.php/dav/trashbin/anna/trash/a.jpg.d5", request.path)
        assertEquals(
            server.url("/remote.php/dav/trashbin/anna/restore/a.jpg").toString(),
            request.getHeader("Destination"),
        )
        assertEquals("F", request.getHeader("Overwrite"))
    }

    @Test
    fun restore_404_failsWithCode() =
        assertFailsWithCode(404) { client.restore(account, TrashItem("a.d5", "a", "/a", 5, null)) }
}
