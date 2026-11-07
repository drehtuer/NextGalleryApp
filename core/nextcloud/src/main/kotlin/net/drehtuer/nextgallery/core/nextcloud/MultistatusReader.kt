package net.drehtuer.nextgallery.core.nextcloud

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** One `d:response`: its href and the values of the propstats with status 200. */
internal data class RawResponse(val href: String, val properties: Map<String, String>, val isCollection: Boolean)

/**
 * The XML side of a multistatus response. What the values mean is up to [MultistatusParser]. The response comes from
 * the network, so it is limited: a folder entry takes around 1 kB of XML, so [maxBytes] allows well over 50,000,
 * and [maxResponses] is the upper limit of what stays in memory. Only the properties the parser reads are kept.
 */
internal class MultistatusReader(
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val maxResponses: Int = DEFAULT_MAX_RESPONSES,
) {
    fun read(stream: InputStream): List<RawResponse> = try {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setInput(LimitedInputStream(stream, maxBytes), null)
        rejectDoctype(parser)
        readResponses(parser)
    } catch (e: XmlPullParserException) {
        throw IOException("The server sent an invalid response", e)
    } catch (e: OutOfMemoryError) {
        throw ResponseTooLargeException(e)
    }

    /**
     * WebDAV has no use for a DOCTYPE, and its entities could expand without end. Only [XmlPullParser.nextToken]
     * reports it; [XmlPullParser.next] skips it. Ends on the root element.
     */
    private fun rejectDoctype(parser: XmlPullParser) {
        var event = parser.eventType
        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.DOCDECL) throw XmlPullParserException("A DOCTYPE is not allowed")
            event = parser.nextToken()
        }
    }

    private fun readResponses(parser: XmlPullParser): List<RawResponse> {
        val responses = mutableListOf<RawResponse>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "response") {
                if (responses.size == maxResponses) throw ResponseTooLargeException()
                readResponse(parser)?.let(responses::add)
            }
            event = parser.next()
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
                in KEPT_PROPERTIES -> properties[name] = readValue(parser)
                else -> skip(parser)
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

    private companion object {
        const val DEFAULT_MAX_BYTES = 64L * 1024L * 1024L
        const val DEFAULT_MAX_RESPONSES = 100_000

        val KEPT_PROPERTIES = setOf(
            "getcontentlength",
            "getcontenttype",
            "getetag",
            "getlastmodified",
            "fileid",
            "permissions",
            "has-preview",
            "contained-file-count",
            "trashbin-filename",
            "trashbin-original-location",
            "trashbin-deletion-time",
        )
    }
}

/** Fails once more than [remaining] bytes have been read. */
private class LimitedInputStream(source: InputStream, private var remaining: Long) : FilterInputStream(source) {
    override fun read(): Int = super.read().also { if (it >= 0) count(1) }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) count(it) }

    private fun count(bytes: Int) {
        remaining -= bytes
        if (remaining < 0) throw ResponseTooLargeException()
    }
}
