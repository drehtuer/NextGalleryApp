package net.drehtuer.nextgallery.core.nextcloud

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream

/** One `d:response`: its href and the values of the propstats with status 200. */
internal data class RawResponse(val href: String, val properties: Map<String, String>, val isCollection: Boolean)

/** The XML side of a multistatus response. What the values mean is up to [MultistatusParser]. */
internal class MultistatusReader {
    fun read(stream: InputStream): List<RawResponse> = try {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setInput(stream, null)
        readResponses(parser)
    } catch (e: XmlPullParserException) {
        throw IOException("The server sent an invalid response", e)
    }

    private fun readResponses(parser: XmlPullParser): List<RawResponse> {
        val responses = mutableListOf<RawResponse>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "response") {
                readResponse(parser)?.let(responses::add)
            }
        }
        return responses
    }

    private fun readResponse(parser: XmlPullParser): RawResponse? {
        var href: String? = null
        val properties = mutableMapOf<String, String>()
        var isCollection = false
        forEachChild(parser) {
            when (parser.name) {
                "href" -> href = parser.nextText().trim()

                "propstat" -> readPropstat(parser)?.let { stat ->
                    properties += stat.properties
                    isCollection = isCollection || stat.isCollection
                }

                else -> skip(parser)
            }
        }
        return href?.let { RawResponse(it, properties, isCollection) }
    }

    private fun readPropstat(parser: XmlPullParser): Propstat? {
        var stat = Propstat(emptyMap(), isCollection = false)
        var succeeded = false
        forEachChild(parser) {
            when (parser.name) {
                "prop" -> stat = readProp(parser)
                "status" -> succeeded = parser.nextText().contains(" 200 ")
                else -> skip(parser)
            }
        }
        return stat.takeIf { succeeded }
    }

    private fun readProp(parser: XmlPullParser): Propstat {
        val properties = mutableMapOf<String, String>()
        var isCollection = false
        forEachChild(parser) {
            when (val name = parser.name) {
                "resourcetype" -> isCollection = readResourceType(parser)
                "checksums" -> forEachChild(parser) { properties["checksum"] = parser.nextText().trim() }
                else -> properties[name] = readValue(parser)
            }
        }
        return Propstat(properties.filterValues { it.isNotEmpty() }, isCollection)
    }

    private fun readResourceType(parser: XmlPullParser): Boolean {
        var isCollection = false
        forEachChild(parser) {
            if (parser.name == "collection") isCollection = true
            skip(parser)
        }
        return isCollection
    }

    /** The text of the current element, and of the elements inside it. */
    private fun readValue(parser: XmlPullParser): String {
        val text = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_DOCUMENT -> throw XmlPullParserException("Unexpected end of document")
            }
        }
        return text.toString().trim()
    }

    /** Calls [block] on the start tag of each child; [block] must consume that child. */
    private inline fun forEachChild(parser: XmlPullParser, block: () -> Unit) {
        val depth = parser.depth
        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) throw XmlPullParserException("Unexpected end of document")
            if (event == XmlPullParser.END_TAG && parser.depth == depth) return
            if (event == XmlPullParser.START_TAG) block()
        }
    }

    private fun skip(parser: XmlPullParser) {
        readValue(parser)
    }

    private data class Propstat(val properties: Map<String, String>, val isCollection: Boolean)
}
