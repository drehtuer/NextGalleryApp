package net.drehtuer.nextgallery.core.nextcloud

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MultistatusParserTest {
    private val parser = MultistatusParser()
    private val root = "https://cloud.example.com/remote.php/dav/files/anna".toHttpUrl()
    private val trashRoot = "https://cloud.example.com/remote.php/dav/trashbin/anna/trash".toHttpUrl()

    private fun multistatus(vararg responses: String) = """<?xml version="1.0"?>
        <d:multistatus xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns" xmlns:nc="http://nextcloud.org/ns">
        ${responses.joinToString("")}
        </d:multistatus>"""

    private fun response(href: String, props: String, missing: String = "") = """<d:response><d:href>$href</d:href>
        <d:propstat><d:prop>$props</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>
        <d:propstat><d:prop>$missing</d:prop><d:status>HTTP/1.1 404 Not Found</d:status></d:propstat>
        </d:response>"""

    private fun parse(xml: String) = parser.parseFiles(xml.byteInputStream(), root)

    private val filePath = "/remote.php/dav/files/anna/Photos/IMG_1.jpg"

    @Test
    fun parseFiles_file_readsAllProperties() {
        val xml = multistatus(
            response(
                filePath,
                """<d:resourcetype/><d:getcontentlength>123</d:getcontentlength>
                <d:getcontenttype>image/jpeg</d:getcontenttype><d:getetag>"abc"</d:getetag>
                <d:getlastmodified>Wed, 21 Oct 2026 07:28:00 GMT</d:getlastmodified><oc:fileid>42</oc:fileid>
                <oc:checksums><oc:checksum>SHA1:aa MD5:bb</oc:checksum></oc:checksums>
                <oc:permissions>RGDNVW</oc:permissions><nc:has-preview>true</nc:has-preview>""",
            ),
        )

        val item = parse(xml).single()

        assertEquals("/Photos/IMG_1.jpg", item.path)
        assertEquals("IMG_1.jpg", item.name)
        assertFalse(item.isDirectory)
        assertEquals(123L, item.size)
        assertEquals("image/jpeg", item.mime)
        assertEquals("abc", item.etag)
        assertEquals(1792567680000L, item.lastModifiedMillis)
        assertEquals(42L, item.fileId)
        assertEquals("SHA1:aa MD5:bb", item.checksums)
        assertEquals("RGDNVW", item.permissions)
        assertTrue(item.hasPreview)
    }

    @Test
    fun parseFiles_folder_isDirectoryWithCount() {
        val xml = multistatus(
            response(
                "/remote.php/dav/files/anna/Photos/",
                "<d:resourcetype><d:collection/></d:resourcetype><nc:contained-file-count>7</nc:contained-file-count>",
            ),
        )

        val item = parse(xml).single()

        assertTrue(item.isDirectory)
        assertEquals("/Photos", item.path)
        assertEquals(7, item.childCount)
    }

    @Test
    fun parseFiles_root_hasRootPath() {
        val xml =
            multistatus(response("/remote.php/dav/files/anna/", "<d:resourcetype><d:collection/></d:resourcetype>"))

        assertEquals("/", parse(xml).single().path)
    }

    @Test
    fun parseFiles_missingProperties_useDefaults() {
        val item = parse(multistatus(response(filePath, "<d:resourcetype/>"))).single()

        assertEquals(0L, item.size)
        assertNull(item.mime)
        assertNull(item.etag)
        assertNull(item.fileId)
        assertNull(item.checksums)
        assertNull(item.permissions)
        assertEquals(0L, item.lastModifiedMillis)
    }

    @Test
    fun parseFiles_propertiesOfFailedPropstat_areIgnored() {
        val xml = multistatus(response(filePath, "<d:resourcetype/>", missing = "<oc:fileid>99</oc:fileid>"))

        assertNull(parse(xml).single().fileId)
    }

    @Test
    fun parseFiles_noPreview_isFalse() {
        val xml = multistatus(response(filePath, "<nc:has-preview>false</nc:has-preview>"))

        assertFalse(parse(xml).single().hasPreview)
    }

    @Test
    fun parseFiles_invalidDate_isZero() {
        val xml = multistatus(response(filePath, "<d:getlastmodified>yesterday</d:getlastmodified>"))

        assertEquals(0L, parse(xml).single().lastModifiedMillis)
    }

    @Test
    fun parseFiles_urlEncodedName_isDecoded() {
        val xml = multistatus(response("/remote.php/dav/files/anna/My%20Photos/a%23b%2B.jpg", "<d:resourcetype/>"))

        val item = parse(xml).single()

        assertEquals("/My Photos/a#b+.jpg", item.path)
        assertEquals("a#b+.jpg", item.name)
    }

    @Test
    fun parseFiles_absoluteHref_isUnderstood() {
        val xml = multistatus(response("https://cloud.example.com$filePath", "<d:resourcetype/>"))

        assertEquals("/Photos/IMG_1.jpg", parse(xml).single().path)
    }

    @Test
    fun parseFiles_hrefOutsideTheRoot_isDropped() {
        val xml = multistatus(response("/remote.php/dav/files/other/secret.jpg", "<d:resourcetype/>"))

        assertTrue(parse(xml).isEmpty())
    }

    @Test
    fun parseFiles_hrefWithDotDot_isDropped() {
        val xml = multistatus(response("/remote.php/dav/files/anna/../../etc/passwd", "<d:resourcetype/>"))

        assertTrue(parse(xml).isEmpty())
    }

    @Test
    fun parseFiles_encodedSlashInName_isDropped() {
        val xml = multistatus(response("/remote.php/dav/files/anna/a%2F..%2Fb.jpg", "<d:resourcetype/>"))

        assertTrue(parse(xml).isEmpty())
    }

    @Test
    fun parseFiles_brokenXml_failsWithIoException() {
        assertThrows(IOException::class.java) { parse("<d:multistatus xmlns:d=\"DAV:\"><d:response>") }
    }

    @Test
    fun parseTrash_readsOriginalLocationAndTime() {
        val xml = multistatus(
            response(
                "/remote.php/dav/trashbin/anna/trash/IMG_1.jpg.d1700000000",
                """<nc:trashbin-filename>IMG_1.jpg</nc:trashbin-filename>
                <nc:trashbin-original-location>Photos/IMG_1.jpg</nc:trashbin-original-location>
                <nc:trashbin-deletion-time>1700000000</nc:trashbin-deletion-time><oc:fileid>42</oc:fileid>""",
            ),
        )

        val item = parser.parseTrash(xml.byteInputStream(), trashRoot).single()

        assertEquals(TrashItem("IMG_1.jpg.d1700000000", "IMG_1.jpg", "/Photos/IMG_1.jpg", 1700000000, 42), item)
    }

    @Test
    fun parseTrash_theTrashFolderItself_isDropped() {
        val xml =
            multistatus(
                response("/remote.php/dav/trashbin/anna/trash/", "<d:resourcetype><d:collection/></d:resourcetype>"),
            )

        assertTrue(parser.parseTrash(xml.byteInputStream(), trashRoot).isEmpty())
    }

    @Test
    fun parseTrash_withoutOriginalLocation_isDropped() {
        val xml = multistatus(response("/remote.php/dav/trashbin/anna/trash/a.d1", "<oc:fileid>1</oc:fileid>"))

        assertTrue(parser.parseTrash(xml.byteInputStream(), trashRoot).isEmpty())
    }

    @Test
    fun parseTrash_originalLocationWithDotDot_isDropped() {
        val xml = multistatus(
            response(
                "/remote.php/dav/trashbin/anna/trash/a.d1",
                "<nc:trashbin-original-location>../other/a.jpg</nc:trashbin-original-location>",
            ),
        )

        assertTrue(parser.parseTrash(xml.byteInputStream(), trashRoot).isEmpty())
    }
}
