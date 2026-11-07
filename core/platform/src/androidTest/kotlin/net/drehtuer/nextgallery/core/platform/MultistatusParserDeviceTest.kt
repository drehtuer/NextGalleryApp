package net.drehtuer.nextgallery.core.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.nextcloud.MultistatusParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/** The unit tests run on kxml2; a device has Android's own XmlPullParser, whose handling of a DOCTYPE matters here. */
@RunWith(AndroidJUnit4::class)
class MultistatusParserDeviceTest {
    private val root = "https://cloud.example.com/remote.php/dav/files/anna".toHttpUrl()

    @Test
    fun parseFiles_doctype_failsTheParse() {
        val xml = """<?xml version="1.0"?><!DOCTYPE d:multistatus [<!ENTITY a "aaaa">]>""" +
            """<d:multistatus xmlns:d="DAV:"></d:multistatus>"""

        assertThrows(IOException::class.java) { MultistatusParser().parseFiles(xml.byteInputStream(), root) }
    }

    @Test
    fun parseFiles_withoutDoctype_works() {
        val xml = """<?xml version="1.0"?><!-- c --><d:multistatus xmlns:d="DAV:"><d:response>
            <d:href>/remote.php/dav/files/anna/a.jpg</d:href><d:propstat><d:prop><d:resourcetype/></d:prop>
            <d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>"""

        assertEquals("/a.jpg", MultistatusParser().parseFiles(xml.byteInputStream(), root).single().path)
    }
}
